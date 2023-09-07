package com.uid2.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientKeyTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void verifyDisabledPropIsOptional() throws Exception {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetTrue() throws Exception {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": true,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertTrue(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetFalse() throws Exception {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"secret\": \"\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }
}
