package com.uid2.shared.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.utils.ObjectMapperFactory;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class OperatorKeyTest {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.build();

    @Test
    public void verifyDisabledPropIsOptional() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"roles\": [ \"OPERATOR\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertFalse(o.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetTrue() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": true,\n" +
                "        \"roles\": [ \"OPERATOR\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertTrue(o.isDisabled());
    }

    @Test
    public void verifyDisabledPropSetFalse() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\" ],\n" +
                "        \"site_id\": 3\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertFalse(o.isDisabled());
    }

    @Test
    public void verifySiteIdPropIsOptionalForBackwardsCompatibility() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\" ]\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertNull(o.getSiteId());
    }

    @Test
    public void verifyRolesPropIsOptionalForBackwardsCompatibility() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(Set.of(Role.OPERATOR), o.getRoles());
    }

    @Test
    public void verifyRolesPropSetOptoutRole() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPTOUT\" ]\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        // Operator role should be set by default
        assertEquals(Set.of(Role.OPERATOR, Role.OPTOUT), o.getRoles());
    }

    @Test
    public void verifyRolesPropSetOperatorRole() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\" ]\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(Set.of(Role.OPERATOR), o.getRoles());
    }

    @Test
    public void verifyRolesPropSetOperatorRoleAndOptoutRole() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPERATOR\", \"OPTOUT\" ]\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(Set.of(Role.OPERATOR, Role.OPTOUT), o.getRoles());
    }

    @Test
    public void verifyRolesPropSetOptoutServiceRole() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"roles\": [ \"OPTOUT_SERVICE\" ]\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(Set.of(Role.OPTOUT_SERVICE), o.getRoles());
    }

    @Test
    public void verifyRolesPropIsWrittenInAlphabeticalOrder() throws JsonProcessingException {
        String expectJson = "{" +
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
        OperatorKey o = new OperatorKey("test-admin-key", "test-keyHash", "test-keySalt", "admin@uid2.com", "admin@uid2.com", "protocol1", 1617149276, false, 1, new HashSet<>(Arrays.asList(Role.OPTOUT, Role.OPERATOR)));

        assertEquals(expectJson, OBJECT_MAPPER.writeValueAsString(o));
    }

    @Test
    public void verifyOperatorTypePropIsOptionalForBackwardsCompatibility() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(OperatorType.PRIVATE, o.getOperatorType());
    }

    @Test
    public void verifyOperatorTypePropIsPublic() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"operator_type\": \"PUBLIC\"\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(OperatorType.PUBLIC, o.getOperatorType());
    }

    @Test
    public void verifyOperatorTypePropIsPrivate() throws JsonProcessingException {
        String testJson = "    {\n" +
                "        \"key\": \"test-admin-key\",\n" +
                "        \"name\": \"admin@uid2.com\",\n" +
                "        \"contact\": \"admin@uid2.com\",\n" +
                "        \"created\": 1617149276,\n" +
                "        \"disabled\": false,\n" +
                "        \"operator_type\": \"PRIVATE\"\n" +
                "    }";
        OperatorKey o = OBJECT_MAPPER.readValue(testJson, OperatorKey.class);

        assertEquals(OperatorType.PRIVATE, o.getOperatorType());
    }

    @ParameterizedTest
    @MethodSource("operatorConstructorArgs")
    public void verifyConstructorSetsPrivateOperatorTypeByDefault(OperatorType type, OperatorKey o) {
        assertEquals(type, o.getOperatorType());
    }

    private static Set<Arguments> operatorConstructorArgs() {
        return Set.of(
                Arguments.of(OperatorType.PRIVATE, new OperatorKey("key1", "test-keyHash1", "test-keySalt1", "name1", "contact1", "protocol1", 1, true)),
                Arguments.of(OperatorType.PRIVATE, new OperatorKey("key2", "test-keyHash2", "test-keySalt2", "name2", "contact2", "protocol2", 2, true, 2)),
                Arguments.of(OperatorType.PRIVATE, new OperatorKey("key3", "test-keyHash3", "test-keySalt3", "name3", "contact3", "protocol3", 3, true, 3,  null)),
                Arguments.of(OperatorType.PUBLIC, new OperatorKey("key4", "test-keyHash4", "test-keySalt4", "name4", "contact4", "protocol4", 4, true, 4,  null, OperatorType.PUBLIC)),
                Arguments.of(OperatorType.PRIVATE, new OperatorKey("key5", "test-keyHash5", "test-keySalt5", "name5", "contact5", "protocol5", 5, true, 5,  null, OperatorType.PRIVATE))
        );
    }
}
