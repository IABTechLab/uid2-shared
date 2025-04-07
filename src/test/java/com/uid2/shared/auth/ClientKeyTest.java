package com.uid2.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.util.Mapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientKeyTest {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Test
    public void verifyDisabledPropIsOptional() throws Exception {
        String testJson = """
                    {
                        "key": "test-admin-key",
                        "secret": "",
                        "name": "admin@uid2.com",
                        "contact": "admin@uid2.com",
                        "created": 1617149276,
                        "roles": [
                            "MAPPER",
                            "GENERATOR"
                        ],
                        "site_id": 3,
                        "service_id": 5
                    }
                """;
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetTrue() throws Exception {
        String testJson = """
                    {
                        "key": "test-admin-key",
                        "secret": "",
                        "name": "admin@uid2.com",
                        "contact": "admin@uid2.com",
                        "created": 1617149276,
                        "disabled": true,
                        "roles": [ "MAPPER", "GENERATOR" ],
                        "site_id": 3,
                        "service_id": 5
                    }
                """;
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertTrue(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetFalse() throws Exception {
        String testJson = """
                    {
                        "key": "test-admin-key",
                        "secret": "",
                        "name": "admin@uid2.com",
                        "contact": "admin@uid2.com",
                        "created": 1617149276,
                        "disabled": false,
                        "roles": [ "MAPPER", "GENERATOR" ],
                        "site_id": 3,
                        "service_id": 5
                    }
                """;
        ClientKey c = OBJECT_MAPPER.readValue(testJson, ClientKey.class);

        assertFalse(c.isDisabled());
    }
}
