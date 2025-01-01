package com.uid2.shared.util;

import io.vertx.core.http.impl.HttpUtils;
import java.util.Set;

public class HTTPPathMetricFilter {
    public static String filterPath(String actualPath, Set<String> pathSet) {
        try {
            String normalized = HttpUtils.normalizePath(actualPath).split("\\?")[0];
            if (normalized.charAt(normalized.length() - 1) == '/') {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            normalized = normalized.toLowerCase();
            return pathSet == null || pathSet.isEmpty() || pathSet.contains(normalized) ? normalized : "/unknown";
        } catch (IllegalArgumentException e) {
            return "/parsing_error";
        }
    }
}
