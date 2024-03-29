package com.uid2.shared.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Mapper {
    private static final ObjectMapper INSTANCE = new ObjectMapper()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Mapper() {
    }

    public static ObjectMapper getInstance() {
        return INSTANCE;
    }
}
