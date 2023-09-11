package com.uid2.shared.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OperatorKeyTest {
    @Test
    public void verifyDisabledPropIsOptional() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"roles\": [ \"OPERATOR\" ],\n" +
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
                "        \"roles\": [ \"OPERATOR\" ],\n" +
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
                "        \"roles\": [ \"OPERATOR\" ],\n" +
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
                "        \"roles\": [ \"OPERATOR\" ]\n" +
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
        Assert.assertEquals(Set.of(Role.OPERATOR), c.getRoles());
    }

    @Test
    public void verifyRolesPropSetOptoutRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPTOUT\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        // Operator role should be set by default
        Assert.assertEquals(Set.of(Role.OPERATOR, Role.OPTOUT), c.getRoles());
    }

    @Test
    public void verifyRolesPropSetOperatorRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertEquals(Set.of(Role.OPERATOR), c.getRoles());
    }

    @Test
    public void verifyRolesPropSetOperatorRoleAndOptoutRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\", \"OPTOUT\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertEquals(Set.of(Role.OPERATOR, Role.OPTOUT), c.getRoles());
    }

    @Test
    public void verifyRolesPropSetOptoutServiceRole() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPTOUT_SERVICE\" ]\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertEquals(Set.of(Role.OPTOUT_SERVICE), c.getRoles());
    }

    @Test
    public void verifyRolesPropIsWrittenInAlphabeticalOrder() throws JsonProcessingException {
        final String expectJson = "{" +
                "\"key\":\"test-admin-key\"," +
                "\"key_hash\":\"test-keyHash\"," +
                "\"key_salt\":\"test-keySalt\"," +
                "\"name\":\"admin@uid2.com\"," +
                "\"contact\":\"admin@uid2.com\"," +
                "\"protocol\":\"protocol1\"," +
                "\"created\":1617149276," +
                "\"disabled\":false," +
                "\"site_id\":1," +
                "\"roles\":[\"OPERATOR\",\"OPTOUT\"]," +
                "\"operator_type\":\"PRIVATE\"" +
                "}";
        OperatorKey k = new OperatorKey("test-admin-key", "test-keyHash", "test-keySalt", "admin@uid2.com", "admin@uid2.com", "protocol1", 1617149276, false, 1, new HashSet<>(Arrays.asList(Role.OPTOUT, Role.OPERATOR)));
        ObjectMapper objectMapper = new ObjectMapper();
        Assert.assertEquals(expectJson, objectMapper.writeValueAsString(k));
    }

    @Test
    public void verifyOperatorTypePropIsOptionalForBackwardsCompatibility() {
        final String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false\n" +
                "    }";

        JsonObject jo = (JsonObject) Json.decodeValue(testJson);
        OperatorKey c = OperatorKey.valueOf(jo);
        Assert.assertEquals(OperatorType.PRIVATE, c.getOperatorType());
    }

    @Test
    public void verifyOperatorTypePropIsPublic() {
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
        Assert.assertEquals(OperatorType.PUBLIC, c.getOperatorType());
    }

    @Test
    public void verifyOperatorTypePropIsPrivate() {
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
        Assert.assertEquals(OperatorType.PRIVATE, c.getOperatorType());
    }

    @Test
    public void verifyConstructorStartsWithPrivateOperator() {
        OperatorKey k1 = new OperatorKey("key1", "test-keyHash1", "test-keySalt1", "name1", "contact1", "protocol1", 1, true);
        Assert.assertEquals(OperatorType.PRIVATE, k1.getOperatorType());
        OperatorKey k2 = new OperatorKey("key2", "test-keyHash2", "test-keySalt2", "name2", "contact2", "protocol2", 2, true, 2);
        Assert.assertEquals(OperatorType.PRIVATE, k2.getOperatorType());
        OperatorKey k3 = new OperatorKey("key3", "test-keyHash3", "test-keySalt3", "name3", "contact3", "protocol3", 3, true, 3,  null);
        Assert.assertEquals(OperatorType.PRIVATE, k3.getOperatorType());
        OperatorKey k4 = new OperatorKey("key4", "test-keyHash4", "test-keySalt4", "name4", "contact4", "protocol4", 4, true, 4,  null, OperatorType.PUBLIC);
        Assert.assertEquals(OperatorType.PUBLIC, k4.getOperatorType());
        OperatorKey k5 = new OperatorKey("key5", "test-keyHash5", "test-keySalt5", "name5", "contact5", "protocol5", 5, true, 5,  null, OperatorType.PRIVATE);
        Assert.assertEquals(OperatorType.PRIVATE, k5.getOperatorType());
    }
}
