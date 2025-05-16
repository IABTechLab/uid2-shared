package com.uid2.shared.audit;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AuditRecord {
    private final Instant timestamp;
    private final String logType;
    private final String source;
    private final int status;
    private final String method;
    private final String endpoint;
    private final String requestId;
    private final JsonObject actor;
    private final String forwardedRequestId;
    private final JsonObject queryParams;
    private final JsonObject requestBody;

    private AuditRecord(Builder builder) {
        this.timestamp = Instant.now();
        this.logType = "audit";
        this.source = AuditRecord.class.getPackage().getName();
        this.status = builder.status;
        this.method = builder.method;
        this.endpoint = builder.endpoint;
        this.requestId = builder.requestId;
        this.actor = builder.actor;
        this.forwardedRequestId = builder.forwardedRequestId;
        this.queryParams = builder.queryParams;
        this.requestBody = builder.requestBody;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("timestamp", timestamp.toString())
                .put("log_type", logType)
                .put("source", source)
                .put("status", status)
                .put("method", method)
                .put("endpoint", endpoint)
                .put("request_id", requestId)
                .put("actor", actor);
        if (forwardedRequestId != null) json.put("forwarded_request_id", forwardedRequestId);
        if (queryParams != null) json.put("query_params", queryParams);
        if (requestBody != null) json.put("request_body", requestBody);
        return json;
    }

    @Override
    public String toString() {
        return toJson().encode();
    }

    public static class Builder {
        private String source;
        private final int status;
        private final String method;
        private final String endpoint;
        private final String requestId;
        private final JsonObject actor;

        private String forwardedRequestId;
        private JsonObject queryParams;
        private JsonObject requestBody;

        public Builder(int status, String method, String endpoint, String requestId, JsonObject actor) {
            this.status = status;
            this.method = method;
            this.endpoint = endpoint;
            this.requestId = requestId;
            this.actor = actor;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder forwardedRequestId(String forwardedRequestId) {
            this.forwardedRequestId = forwardedRequestId;
            return this;
        }

        public Builder queryParams(JsonObject queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder requestBody(JsonObject requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public AuditRecord build() {
            return new AuditRecord(this);
        }
    }
}

public class Audit {
    private static final Logger LOGGER = LoggerFactory.getLogger(Audit.class);

    private static Set<String> flattenToDotNotation(JsonObject json, String parentKey) {
        Set<String> keys = new HashSet<>();

        for (Map.Entry<String, Object> entry : json) {
            String fullKey = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof JsonObject) {
                keys.addAll(flattenToDotNotation((JsonObject) value, fullKey));
            } else {
                keys.add(fullKey);
            }
        }

        return keys;
    }

    private static void removeByDotKey(JsonObject json, String dotKey) {
        String[] parts = dotKey.split("\\.");
        JsonObject current = json;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.getValue(parts[i]);
            if (!(next instanceof JsonObject)) {
                return;
            }
            current = (JsonObject) next;
        }
        current.remove(parts[parts.length - 1]);
    }

    private JsonObject filterQueryParams(MultiMap queryParamsMap, List<String> queryParams) {
        JsonObject queryParamsJson = new JsonObject();
        queryParamsMap.forEach(entry -> {
            if ( queryParams.contains(entry.getKey())) {
                queryParamsJson.put(entry.getKey(), entry.getValue());
            }
        });
        return queryParamsJson;
    }

    private JsonObject filterBody(JsonObject bodyJson, List<String> bodyParams) {
        Set<String> allowedKeys = bodyParams != null
                ? new HashSet<>(bodyParams): null;
        if (bodyJson != null && allowedKeys !=null ) {
            Set<String> dotKeys = flattenToDotNotation(bodyJson, "");
            for (String key : dotKeys) {
                if (!allowedKeys.contains(key)) {
                    removeByDotKey(bodyJson, key);
                }
            }
        }
        return bodyJson;
    }

    private String defaultIfNull(String s) {
        return s != null ? s : "unknown";
    }

    public void log(RoutingContext ctx, AuditParams params) {

        JsonObject userDetails = ctx.get("userDetails");

        if (userDetails == null) {
            userDetails = new JsonObject();
        }

        try {
            HttpServerRequest request = ctx.request();
            HttpServerResponse response = ctx.response();

            userDetails.put("User-Agent", defaultIfNull(request.getHeader("User-Agent")));
            userDetails.put("IP", request.remoteAddress() != null ? request.remoteAddress().host() : "unknown");


            int status = response != null ? response.getStatusCode() : -1;
            String method = request.method() != null ? request.method().name() : "UNKNOWN";
            String path = defaultIfNull(request.path());
            String traceId = defaultIfNull(request.getHeader("X-Amzn-Trace-Id"));


            AuditRecord.Builder builder = new AuditRecord.Builder(
                    status,
                    method,
                    path,
                    traceId,
                    userDetails
            );
            if (params != null) {
                JsonObject bodyJson = filterBody(ctx.body().asJsonObject(), params.bodyParams());
                JsonObject queryParamsJson = filterQueryParams(ctx.request().params(), params.queryParams());
                if (!queryParamsJson.isEmpty()) {
                    builder.queryParams(queryParamsJson);
                }

                if (bodyJson != null && !bodyJson.isEmpty()) {
                    builder.requestBody(bodyJson);
                }
            }

            if (ctx.request().getHeader("UID2-Forwarded-Trace-Id") != null) {
                builder.forwardedRequestId(ctx.request().getHeader("UID2-Forwarded-Trace-Id"));
            }

            AuditRecord auditRecord = builder.build();
            LOGGER.info(auditRecord.toString());
        } catch (Exception e) {
            LOGGER.warn("Failed to log audit record", e);
        }
    }

}
