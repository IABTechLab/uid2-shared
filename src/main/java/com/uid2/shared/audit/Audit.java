package com.uid2.shared.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
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
        private final StringBuilder toJsonValidationErrorMessageBuilder = new StringBuilder();
        @Getter
        private String toJsonValidationErrorMessage = "";

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
                    .put("endpoint", endpoint);

            if (traceId != null && validateId(traceId, "trace_id")) {
                json.put("trace_id", traceId);
            }

            if (uidTraceId != null && validateId(uidTraceId, "uid_trace_id")) {
                json.put("uid_trace_id", uidTraceId);
            }

            if (uidInstanceId != null && validateId(uidInstanceId, "uid_instance_id")) {
                json.put("uid_instance_id", uidInstanceId);
            }
            actor.put("id", this.getLogIdentifier(json));
            if (validateJsonObjectParams(actor, "actor")) {
                json.put("actor", actor);
            }
            if (queryParams != null && validateJsonObjectParams(queryParams, "query_params")) json.put("query_params", queryParams);
            if (requestBody != null) {
                String sanitizedRequestBody = sanitizeRequestBody(requestBody);
                if (!sanitizedRequestBody.isEmpty()) {
                    json.put("request_body", sanitizedRequestBody);
                }
            }
            toJsonValidationErrorMessage = toJsonValidationErrorMessageBuilder.isEmpty()? "" : "Audit log failure: " + toJsonValidationErrorMessageBuilder.toString();
            return json;
        }

        private boolean validateJsonObjectParams(JsonObject jsonObject, String propertyName) {
            Set<String> keysToRemove = new HashSet<>();

            for (String key : jsonObject.fieldNames()) {
                String val = jsonObject.getString(key);

                boolean containsNoSecret = validateNoSecrets(key, propertyName) && validateNoSecrets(val, propertyName);
                boolean containsNoSQL = validateNoSQL(key, propertyName) && validateNoSQL(val, propertyName);

                if (!(containsNoSecret && containsNoSQL)) {
                    keysToRemove.add(key);
                }

                int parameter_max_length = 1000;
                if (val != null && val.length() > parameter_max_length) {
                    val = val.substring(0, parameter_max_length);
                    toJsonValidationErrorMessageBuilder.append(String.format(
                            "The %s is too long in the audit log: %s. ", propertyName, key));
                }

                jsonObject.put(key, val);
            }

            for (String key : keysToRemove) {
                jsonObject.remove(key);
            }

            return !jsonObject.isEmpty();
        }

        private boolean validateJsonArrayParams(JsonArray jsonArray, String propertyName) {
            JsonArray newJsonArray = new JsonArray();

            for (Object object : jsonArray) {
                if (object instanceof JsonObject) {
                    if (validateJsonObjectParams((JsonObject)object, propertyName)) {
                        newJsonArray.add(object);
                    }
                } else {
                    toJsonValidationErrorMessageBuilder.append("The request body is a JSON array, but one of its elements is not a JSON object.");
                }
            }

            jsonArray.clear();
            jsonArray.addAll(newJsonArray);

            return !jsonArray.isEmpty();
        }

        private String sanitizeRequestBody(String requestBody) {
            ObjectMapper mapper = new ObjectMapper();
            String sanitizedRequestBody = "";

            try {
                JsonNode root = mapper.readTree(requestBody);

                if (root.isObject()) {
                    JsonObject jsonObject = new JsonObject(mapper.writeValueAsString(root));
                    if (validateJsonObjectParams(jsonObject, "request_body")) sanitizedRequestBody = jsonObject.toString();
                } else if (root.isArray()) {
                    JsonArray jsonArray = new JsonArray(mapper.writeValueAsString(root));
                    if (validateJsonArrayParams(jsonArray, "request_body")) sanitizedRequestBody = jsonArray.toString();
                } else {
                    toJsonValidationErrorMessageBuilder.append("The request body of audit log is not a JSON object or array. ");
                }
            } catch (Exception e) {
                toJsonValidationErrorMessageBuilder.append("The request body of audit log is Invalid JSON: ").append(e.getMessage());

            }

            int request_body_max_length = 10000;
            if (sanitizedRequestBody.length() > request_body_max_length) {
                sanitizedRequestBody = sanitizedRequestBody.substring(0, request_body_max_length);
                toJsonValidationErrorMessageBuilder.append("Request body is too long in the audit log: %s. ");
            }
            return sanitizedRequestBody;
        }

        private boolean validateNoSecrets(String fieldValue, String propertyName) {
            if (fieldValue == null || fieldValue.isEmpty()) {
                return true;
            }
            Pattern uid2_key_pattern = Pattern.compile("(UID2|EUID)-[A-Za-z]-[A-Za-z]-[A-Za-z0-9_-]+");
            Matcher matcher = uid2_key_pattern.matcher(fieldValue);
            if(matcher.find()) {
                toJsonValidationErrorMessageBuilder.append(String.format("Secret found in the audit log: %s. ", propertyName));
                return false;
            } else {
                return true;
            }
        }

        private boolean validateNoSQL(String fieldValue, String propertyName) {
            if (fieldValue == null || fieldValue.isEmpty()) {
                return true;
            }
            Pattern sql_injection_pattern = Pattern.compile(
                    "(?i)(\\bselect\\b\\s+.+\\s+\\bfrom\\b|\\bunion\\b\\s+\\bselect\\b|\\binsert\\b\\s+\\binto\\b|\\bdrop\\b\\s+\\btable\\b|--|#|\\bor\\b|\\band\\b|\\blike\\b|\\bin\\b\\s*\\(|;)"
            );
            if(sql_injection_pattern.matcher(fieldValue).find()) {
                toJsonValidationErrorMessageBuilder.append(String.format("SQL injection found in the audit log: %s. ", propertyName));
                return false;
            } else {
                return true;
            }
        }

        private boolean validateId(String uidInstanceId, String propertyName) {
            if(uidInstanceId.length() < 100 && validateNoSecrets(uidInstanceId, propertyName) && validateNoSQL(uidInstanceId, propertyName) ) {
                return true;
            } else {
                toJsonValidationErrorMessageBuilder.append(String.format("Malformed %s found in the audit log. ", propertyName));
                return false;
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
    private static final String UNKNOWN_ID = "unknown";

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
        return s != null ? s : UNKNOWN_ID;
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
            String headerValue = request.getHeader(UID_TRACE_ID_HEADER);
            if (headerValue != null && "unknown".equalsIgnoreCase(headerValue)) {
                LOGGER.error("UID_TRACE_ID_HEADER is unknown -- ABU DEBUG");
            }
            String uidTraceId = (headerValue == null || "unknown".equalsIgnoreCase(headerValue)) ? traceId : headerValue;
            //String uidTraceId = defaultIfNull(request.getHeader(UID_TRACE_ID_HEADER), traceId);
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
            String auditRecordString = auditRecord.toString();
            if (!auditRecord.getToJsonValidationErrorMessage().isEmpty()) {
                LOGGER.error(auditRecord.getToJsonValidationErrorMessage() + auditRecordString);
            }
            LOGGER.info(auditRecordString);

        } catch (Exception e) {
            LOGGER.warn("Failed to log audit record", e);
        }
    }

}
