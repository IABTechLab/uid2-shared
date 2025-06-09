package com.uid2.shared.util;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public class LogFmtLineBuilder {
    private final StringBuilder stringBuilder;

    public LogFmtLineBuilder() {
        this.stringBuilder = new StringBuilder();
    }

    private String escape(String value) {
        if (value == null) {
            return "null";
        }

        if (value.contains(" ") || value.contains("\"") || value.contains("=") || value.contains("\n") ||
                value.contains("\r") || value.contains("\t") || value.contains("\\")) {
            return "\"" + value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + "\"";
        }

        return value;
    }

    public LogFmtLineBuilder with(String key, String value) {
        if (!stringBuilder.isEmpty()) {
            stringBuilder.append(" ");
        }
        stringBuilder.append(key).append("=").append(escape(value));
        return this;
    }

    public LogFmtLineBuilder with(Map<String, String> map) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                with(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public LogFmtLineBuilder with(String key, int value) {
        return with(key, String.valueOf(value));
    }

    // Only supports one level of nesting
    public LogFmtLineBuilder with(String key, JsonObject obj) {
        for (String objKey : obj.fieldNames()) {
            with(key+ "." + objKey, obj.getString(objKey));
        }
        return this;
    }

    public String build() {
        return stringBuilder.toString();
    }
}
