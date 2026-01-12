package com.uid2.shared.util;

import com.uid2.shared.Utils;
import java.util.Set;

public class HTTPPathMetricFilter {
    public static String filterPath(String actualPath, Set<String> pathSet) {
        try {
            String normalized = Utils.getNormalizedHttpPath(actualPath);

            if (pathSet == null || pathSet.isEmpty()) { return normalized; }

            for (String path : pathSet) {
                String pathRegex = path.replaceAll(":[^/]+", "[^/]+");
                if (normalized.matches(pathRegex)) {
                    return path;
                }
            }
            return "/unknown";
        } catch (IllegalArgumentException e) {
            return "/parsing_error";
        }
    }

    public static String filterPathWithoutPathParameters(String actualPath, Set<String> pathSet) {
        try {
            String normalized = Utils.getNormalizedHttpPath(actualPath);

            if (pathSet == null || pathSet.isEmpty()) { return normalized; }

            if (pathSet.contains(normalized)) { return normalized; }

            return "/unknown";
        } catch (IllegalArgumentException e) {
            return "/parsing_error";
        }
    }
}
