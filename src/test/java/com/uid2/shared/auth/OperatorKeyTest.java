package com.uid2.shared.auth;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

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
        Assert.assertFalse(c.hasRole(Role.OPTOUT));
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
        Assert.assertTrue(c.hasRole(Role.OPTOUT));
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
        Assert.assertTrue(c.getOperatorType() == OperatorType.PRIVATE);

        c.setOperatorType(OperatorType.PUBLIC);
        Assert.assertTrue(c.getOperatorType() == OperatorType.PUBLIC);
    }

    @Test
    public void verifyPublicOperatorType() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"operator_type\": \"PUBLIC\"\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.getOperatorType() == OperatorType.PUBLIC);

        c.setOperatorType(OperatorType.PRIVATE);
        Assert.assertTrue(c.getOperatorType() == OperatorType.PRIVATE);
    }

    @Test
    public void verifyPrivateOperatorType() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"operator_type\": \"PRIVATE\"\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertTrue(c.getOperatorType() == OperatorType.PRIVATE);

        c.setOperatorType(OperatorType.PUBLIC);
        Assert.assertTrue(c.getOperatorType() == OperatorType.PUBLIC);
    }

    @Test
    public void verifyConstructorStartsWithPrivateOperator() {
        OperatorKey k1 = new OperatorKey("key1", "name1", "contact1", "protocol1", 1, true);
        Assert.assertTrue(k1.getOperatorType() == OperatorType.PRIVATE);
        OperatorKey k2 = new OperatorKey("key2", "name2", "contact2", "protocol2", 2, true, 2);
        Assert.assertTrue(k2.getOperatorType() == OperatorType.PRIVATE);
        OperatorKey k3 = new OperatorKey("key3", "name3", "contact3", "protocol3", 3, true, 3,  new HashSet<Role>());
        Assert.assertTrue(k3.getOperatorType() == OperatorType.PRIVATE);
        OperatorKey k4 = new OperatorKey("key4", "name4", "contact4", "protocol4", 4, true, 4,  new HashSet<Role>(), OperatorType.PUBLIC);
        Assert.assertTrue(k4.getOperatorType() == OperatorType.PUBLIC);
        OperatorKey k5 = new OperatorKey("key5", "name5", "contact5", "protocol5", 5, true, 5,  new HashSet<Role>(), OperatorType.PRIVATE);
        Assert.assertTrue(k5.getOperatorType() == OperatorType.PRIVATE);
    }
}
