package com.uid2.shared.audit;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import java.time.Instant;
import java.util.*;

public class Audit {


    static class AuditRecord {
        private final Instant timestamp;
        private final String logType;
        private final String source;
        private final int status;
        private final String method;
        private final String endpoint;
        private final String traceId;
        private final JsonObject actor;
        private final String uidTraceId;
        private final JsonObject queryParams;
        private final JsonObject requestBody;

        private AuditRecord(Builder builder) {
            this.timestamp = Instant.now();
            this.logType = "audit";
            this.source = builder.source;
            this.status = builder.status;
            this.method = builder.method;
            this.endpoint = builder.endpoint;
            this.traceId = builder.traceId;
            this.actor = builder.actor;
            this.uidTraceId = builder.uidTraceId;
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
                    .put("trace_id", traceId)
                    .put("actor", actor);
            if (uidTraceId != null) {
                json.put("uid_trace_id", uidTraceId);
            }
            if (queryParams != null) json.put("query_params", queryParams);
            if (requestBody != null) json.put("request_body", requestBody);
            return json;
        }

        @Override
        public String toString() {
            return toJson().encode();
        }

        public static class Builder {
            private final int status;
            private final String method;
            private final String endpoint;
            private final String traceId;
            private final JsonObject actor;
            private final String source;

            private String uidTraceId;
            private JsonObject queryParams;
            private JsonObject requestBody;

            public Builder(int status, String source, String method, String endpoint, String traceId, String uidTraceId, JsonObject actor) {
                this.status = status;
                this.source = source;
                this.method = method;
                this.endpoint = endpoint;
                this.traceId = traceId;
                this.uidTraceId = uidTraceId != null ? uidTraceId : traceId;
                this.actor = actor;
            }

            public Builder uid2RequestId(String uid2RequestId) {
                this.uidTraceId = uid2RequestId;
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

    private final String source;

    public Audit (String source) {
        this.source = source;
    }

    public static final String UID_TRACE_ID_HEADER = "UID-Trace-Id";
    public static final String UID_INSTANCE_ID_HEADER = "UID-Instance-Id";
    private static final Logger LOGGER = LoggerFactory.getLogger(Audit.class);

    private static Set<String> flattenToDotNotation(JsonObject json, String parentKey) {
        Set<String> keys = new HashSet<>();

        for (Map.Entry<String, Object> entry : json) {
            String fullKey = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof JsonObject jsonObject) {
                keys.addAll(flattenToDotNotation(jsonObject, fullKey));
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
        if (queryParamsMap == null) return queryParamsJson;
        queryParamsMap.forEach(entry -> {
            if (queryParams.contains(entry.getKey())) {
                queryParamsJson.put(entry.getKey(), entry.getValue());
            }
        });
        return queryParamsJson;
    }

    private JsonObject filterBody(JsonObject bodyJson, List<String> bodyParams) {
        Set<String> allowedKeys = bodyParams != null
                ? new HashSet<>(bodyParams): null;
        if (bodyJson == null || allowedKeys == null) {
            return new JsonObject();
        }
        Set<String> dotKeys = flattenToDotNotation(bodyJson, "");
        for (String key : dotKeys) {
            if (!allowedKeys.contains(key)) {
                removeByDotKey(bodyJson, key);
            }
        }
        return bodyJson;
    }

    private String defaultIfNull(String s) {
        return s != null ? s : "unknown";
    }

    public static final String USER_DETAILS = "user_details";

    public void log(RoutingContext ctx, AuditParams params) {
        Objects.requireNonNull(ctx, "RoutingContext must not be null");
        Objects.requireNonNull(params, "AuditParams must not be null");

        JsonObject userDetails = ctx.get(USER_DETAILS);

        if (userDetails == null) {
            userDetails = new JsonObject();
        }

        try {
            HttpServerRequest request = ctx.request();
            HttpServerResponse response = ctx.response();

            userDetails.put("user_agent", defaultIfNull(request.getHeader("User-Agent")));
            userDetails.put("ip", defaultIfNull(request.remoteAddress() != null ? request.remoteAddress().host() : null));

            int status = response != null ? response.getStatusCode() : -1;
            String method = request.method() != null ? request.method().name() : "UNKNOWN";
            String path = defaultIfNull(request.path());
            String traceId = defaultIfNull(request.getHeader("X-Amzn-Trace-Id"));
            String uidTraceId = defaultIfNull(request.getHeader(UID_TRACE_ID_HEADER));


            AuditRecord.Builder builder = new AuditRecord.Builder(
                    status,
                    this.source,
                    method,
                    path,
                    traceId,
                    uidTraceId,
                    userDetails
            );

            JsonObject bodyJson = null;
            if (ctx.body() != null && ctx.body().asJsonObject() != null && params.bodyParams() != null) {
                bodyJson = filterBody(ctx.body().asJsonObject(), params.bodyParams());
            }

            JsonObject queryParamsJson = null;
            if (ctx.request() != null && ctx.request().params() != null && params.queryParams() != null) {
                queryParamsJson = filterQueryParams(ctx.request().params(), params.queryParams());
            }

            if (queryParamsJson != null && !queryParamsJson.isEmpty()) {
                builder.queryParams(queryParamsJson);
            }

            if (bodyJson != null &&  !bodyJson.isEmpty()) {
                builder.requestBody(bodyJson);
            }

            AuditRecord auditRecord = builder.build();
            LOGGER.info(auditRecord.toString());
        } catch (Exception e) {
            LOGGER.warn("Failed to log audit record", e);
        }
    }

}
