package com.uid2.shared.auth;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class OperatorKeyTest {
    @Test
    public void verifyDisabledPropIsOptional() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertFalse(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetTrue() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": true,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetFalse() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertFalse(c.isDisabled());
    }

    @Test
    public void verifySiteIdPropIsOptionalForBackwardsCompatibility() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"mapper\", \"generator\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertNull(c.getSiteId());
    }

    @Test
    public void verifyIsPublicOperatorFlagIsOptionalForBackwardsCompatibility() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.isPrivateOperator());
        Assert.assertFalse(c.isPublicOperator());

        c.setIsPublicOperator(true);
        Assert.assertTrue(c.isPublicOperator());
        Assert.assertFalse(c.isPrivateOperator());
    }
}
