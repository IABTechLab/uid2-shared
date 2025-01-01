package com.uid2.shared.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HTTPPathMetricFilterTest {
    final Set<String> pathSet = Set.of("/v1/identity/map", "/token/refresh");

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "/",
            "/unknown-path",
            "../",
            "/v1/identity/map%55",
    })
    void testPathFiltering_InvalidPaths_Unknown(String actualPath) {
        String filteredPath = HTTPPathMetricFilter.filterPath(actualPath, pathSet);
        assertEquals("/unknown", filteredPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "v1/identity/map?id=bad-escape-code%2",
            "token/refresh?refresh_token=SOME_TOKEN<%=7485*4353%>",
    })
    void testPathFiltering_InvalidPaths_ParsingError(String actualPath) {
        String filteredPath = HTTPPathMetricFilter.filterPath(actualPath, pathSet);
        assertEquals("/parsing_error", filteredPath);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "/v1/identity/map, /v1/identity/map",
            "v1/identity/map, /v1/identity/map",
            "V1/IdenTity/mAp, /v1/identity/map",
            "./v1//identity//map/, /v1/identity/map",
            "../v1/identity/./map, /v1/identity/map",
            "/v1/identity/new/path/../../map, /v1/identity/map",
            "token/refresh?refresh_token=123%20%23, /token/refresh",
            "v1/identity/map?identity/../map/, /v1/identity/map",

    })
    void testPathFiltering_ValidPaths_KnownEndpoints(String actualPath, String expectedFilteredPath) {
        String filteredPath = HTTPPathMetricFilter.filterPath(actualPath, pathSet);
        assertEquals(expectedFilteredPath, filteredPath);
    }
}
