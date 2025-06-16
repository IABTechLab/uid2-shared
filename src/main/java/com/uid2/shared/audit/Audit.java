package com.uid2.shared.audit;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        private final String uidInstanceId;
        private final String requestBody;
        private static final Pattern UID2_KEY_PATTERN = Pattern.compile("(UID2|EUID)-[A-Za-z]-[A-Za-z]-[A-Za-z0-9_-]+");
        private static final int PARAMETER_MAX_LENGTH = 1000;
        private static final int REQUEST_BODY_MAX_LENGTH = 10000;
        private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
                "(select\\s+.+\\s+from|union\\s+select|insert\\s+into|drop\\s+table|--|#|\\bor\\b|\\band\\b|\\blike\\b|\\bin\\b\\s*\\(|;)"
        );
        private static final Pattern X_Amzn_Trace_Id_PATTERN = Pattern.compile(
                "(?:Root|Self)=1-[0-9a-fA-F]{8}-[0-9a-fA-F]{24}\n"
        ); // https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-request-tracing.html
        private static final Pattern UID_INSTANCE_ID_PATTERN = Pattern.compile(
                "i-[a-f0-9]+-ami-[a-f0-9]+|" + // AWS
                        "gcp-\\d+-project/[a-z0-9-]+/images/operator:\\d+\\.\\d+|" + // GCP
                        "azure-\\d+-\\d+-UID2-operator-\\d+\\.\\d+" +  // Azure
                        "^(uid2|euid)-+" + // Kubernetes
                        "unknown"
        );

        private final StringBuilder errorMessageBuilder = new StringBuilder();
        private String errorMessage = "";

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
            this.uidInstanceId = builder.uidInstanceId;
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
                    .put("trace_id", traceId);

            if (traceId != null || validateAmazonTraceId(traceId, "trace_id")) {
                json.put("trace_id", traceId);
            }

            if (uidTraceId != null || validateAmazonTraceId(uidTraceId, "uid_trace_id")) {
                json.put("uid_trace_id", uidTraceId);
            }

            if (uidInstanceId != null || validateUIDInstanceId(uidInstanceId)) {
                json.put("uid_instance_id", uidInstanceId);
            }
            actor.put("id", this.getLogIdentifier(json));
            if (validateJsonParams(actor, "actor")) {
                json.put("actor", actor);
            }
            if (queryParams != null && validateJsonParams(queryParams, "query_params")) json.put("query_params", queryParams);
            if (requestBody != null && validateRequestBody(requestBody)) json.put("request_body", requestBody);
            errorMessage = errorMessageBuilder.toString();
            return json;
        }

        private boolean validateJsonParams(JsonObject jsonObject, String propertyName) {
            for (String key : jsonObject.fieldNames()) {
                String val = jsonObject.getString(key);
                if (val != null) {
                    if (val.length() > PARAMETER_MAX_LENGTH) {
                        val = val.substring(0, PARAMETER_MAX_LENGTH);
                        errorMessageBuilder.append(String.format("The %s is too long in the audit log: %s. ", propertyName, key));
                    }
                    jsonObject.put(key, val);
                }
            }

            String jsonObjectString = jsonObject.toString();
            boolean noSecret = validateNoSecrets(jsonObjectString, propertyName);
            boolean noSQL = validateNoSQL(jsonObjectString, propertyName);
            return noSQL && noSecret;
        }

        private boolean validateRequestBody(String requestBody ) {
            if (requestBody != null && requestBody.length() > REQUEST_BODY_MAX_LENGTH) {
                requestBody = requestBody.substring(0, REQUEST_BODY_MAX_LENGTH);
                errorMessageBuilder.append("Request body is too long in the audit log: %s. ");
            }
            boolean noSecret = validateNoSecrets(requestBody, "request_body");
            boolean noSQL = validateNoSQL(requestBody, "request_body");
            return noSQL && noSecret;
        }

        private boolean validateNoSecrets(String fieldValue, String propertyName) {
            if (fieldValue == null || fieldValue.isEmpty()) {
                return true;
            }
            Matcher matcher = UID2_KEY_PATTERN.matcher(fieldValue);
            if(matcher.find()) {
                errorMessageBuilder.append(String.format("Secret found in the audit log: %s. ", propertyName));
                return false;
            } else {
                return true;
            }
        }

        private boolean validateNoSQL(String fieldValue, String propertyName) {
            if (fieldValue == null || fieldValue.isEmpty()) {
                return true;
            }
            if(SQL_INJECTION_PATTERN.matcher(fieldValue).find()) {
                errorMessageBuilder.append(String.format("SQL injection found in the audit log: %s. ", propertyName));
                return false;
            } else {
                return true;
            }
        }

        private boolean validateUIDInstanceId(String uidInstanceId) {
            if(UID_INSTANCE_ID_PATTERN.matcher(uidInstanceId).find()) {
                errorMessageBuilder.append("Malformed uid_instance_id found in the audit log. ");
                return false;
            } else {
                return true;
            }
        }

        private boolean validateAmazonTraceId(String traceId, String propertyName) {
            if(UID_INSTANCE_ID_PATTERN.matcher(traceId).find()) {
                errorMessageBuilder.append(String.format("Malformed %s found in the audit log. ", propertyName));
                return false;
            } else {
                return true;
            }
        }

        private String getLogIdentifier(JsonObject logObject) {
            JsonObject actor = logObject.getJsonObject("actor");
            String email = (actor != null) ? actor.getString("email") : null;
            if (email != null && !email.isEmpty()) {
                return email;
            }
            return logObject.getString("uid_instance_id");
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
            private final String uidTraceId;
            private final String uidInstanceId;

            private JsonObject queryParams;
            private String requestBody;

            public Builder(int status, String source, String method, String endpoint, String traceId, String uidTraceId, JsonObject actor, String uidInstanceId) {
                this.status = status;
                this.source = source;
                this.method = method;
                this.endpoint = endpoint;
                this.traceId = traceId;
                this.uidTraceId = uidTraceId;
                this.actor = actor;
                this.uidInstanceId = uidInstanceId;
            }

            public Builder queryParams(JsonObject queryParams) {
                this.queryParams = queryParams;
                return this;
            }

            public Builder requestBody(String requestBody) {
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

    private String getBody(RequestBody requestBody, List<String> bodyParams) {
        if (requestBody == null) {
            return "";
        }

        Buffer bodyBuffer = requestBody.buffer();
        if (bodyBuffer == null || bodyBuffer.length() == 0) {
            return "";
        }

        Set<String> allowedKeys = bodyParams != null
                ? new HashSet<>(bodyParams): null;
        if (allowedKeys == null) {
            return "";
        }

        try {
            Object genericJsonValue = Json.decodeValue(bodyBuffer);
            if (genericJsonValue instanceof JsonObject) {
                return filterBody((JsonObject) genericJsonValue, allowedKeys);
            } else if (genericJsonValue instanceof JsonArray) {
                return filterJsonArrayBody((JsonArray) genericJsonValue, allowedKeys);
            } else {
                return "";
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse body param", e);
            return "";
        }
    }

    private String filterBody(JsonObject bodyJson, Set<String> allowedKeys) {
        if (bodyJson == null) {
            return "";
        }

        Set<String> dotKeys = flattenToDotNotation(bodyJson, "");
        for (String key : dotKeys) {
            if (!allowedKeys.contains(key)) {
                removeByDotKey(bodyJson, key);
            }
        }
        return bodyJson.toString();
    }

    private String filterJsonArrayBody(JsonArray bodyJson, Set<String> allowedKeys) {
        if (bodyJson == null) {
            return "";
        }

        JsonArray newJsonArray = new JsonArray();
        for (Object object : bodyJson) {
            if (object instanceof JsonObject) {
                JsonObject jsonObject = (JsonObject) object;

                Set<String> dotKeys = flattenToDotNotation(jsonObject, "");
                for (String key : dotKeys) {
                    if (!allowedKeys.contains(key)) {
                        removeByDotKey(jsonObject, key);
                    }
                }
                newJsonArray.add(jsonObject);
            }
        }
        return newJsonArray.toString();
    }

    private String defaultIfNull(String s) {
        return s != null ? s : "unknown";
    }

    private String defaultIfNull(String s, String defaultValue) {
        return s != null ? s : defaultValue;
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
            String uidTraceId = defaultIfNull(request.getHeader(UID_TRACE_ID_HEADER), traceId);
            String uidInstanceId = defaultIfNull(request.getHeader(UID_INSTANCE_ID_HEADER));


            AuditRecord.Builder builder = new AuditRecord.Builder(
                    status,
                    this.source,
                    method,
                    path,
                    traceId,
                    uidTraceId,
                    userDetails,
                    uidInstanceId
            );

            String bodyJson = getBody(ctx.body(), params.bodyParams());

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
            if (auditRecord.errorMessage.isEmpty()) {
                LOGGER.error(auditRecord.errorMessage + auditRecord);
            } else {
                LOGGER.info(auditRecord.toString());
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to log audit record", e);
        }
    }

}
