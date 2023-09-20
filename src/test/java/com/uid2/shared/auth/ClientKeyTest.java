package com.uid2.shared.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.utils.Mapper;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientKeyTest {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Test
    public void verifyDisabledPropIsOptional() throws JsonProcessingException {
        String testJson = "{\n" +
                "    \"key\": \"test-admin-key\",\n" +
                "    \"secret\": \"\",\n" +
                "    \"name\": \"admin@uid2.com\",\n" +
                "    \"contact\": \"admin@uid2.com\",\n" +
                "    \"created\": 1617149276,\n" +
                "    \"roles\": [\n" +
                "        \"MAPPER\",\n" +
                "        \"GENERATOR\"\n" +
                "    ],\n" +
                "    \"site_id\": 3,\n" +
                "    \"service_id\": 5\n" +
                "}";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetTrue() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": true,\n" +
                "        \"roles\": [ \"MAPPER\", \"GENERATOR\" ],\n" +
                "        \"site_id\": 3,\n" +
                "        \"service_id\": 5\n" +
                "    }";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertTrue(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetFalse() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"MAPPER\", \"GENERATOR\" ],\n" +
                "        \"site_id\": 3,\n" +
                "        \"service_id\": 5\n" +
                "    }";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }
}
