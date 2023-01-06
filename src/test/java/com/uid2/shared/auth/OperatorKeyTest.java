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
    public void verifyRolesPropIsOptionalForBackwardsCompatibility() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.getRoles().contains(Role.OPERATOR));
    }
    @Test
    public void verifyRolesPropSetOptoutRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"optout\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        // Operator role should be set by default
        Assert.assertTrue(c.getRoles().contains(Role.OPERATOR));
        Assert.assertTrue(c.getRoles().contains(Role.OPTOUT));
    }

    @Test
    public void verifyRolesPropSetOperatorRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"operator\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.getRoles().contains(Role.OPERATOR));
        Assert.assertFalse(c.hasRole(Role.OPTOUT));
    }

    @Test
    public void verifyRolesPropSetOperatorRoleAndOptoutRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"operator\", \"optout\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.getRoles().contains(Role.OPERATOR));
        Assert.assertTrue(c.hasRole(Role.OPTOUT));
    }
}
