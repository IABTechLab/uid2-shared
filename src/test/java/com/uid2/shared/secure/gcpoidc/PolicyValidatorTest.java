package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;
import com.uid2.shared.util.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyValidatorTest {
    private static final String ATTESTATION_URL = "https://core.uidapi.com";
    private static final String OPT_OUT_URL = "https://optout.uidapi.com";
    private static final String OTHER_OPT_OUT_URL = "https://optout.example.com";

    @Test
    public void testValidationSuccess_FullProd() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload);
        assertNotNull(enclaveId);
    }

    @Test
    public void testValidationFailure_MissRequiredEnvProd() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.remove(PolicyValidator.ENV_ENVIRONMENT);
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(newPayload));
        assertInstanceOf(AttestationClientException.class, e);
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_UnknownEnv() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "env1");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(newPayload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void testValidationSuccess_IntegNoOptionalEnv() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Integration, true);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        payload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var enclaveId = validator.validate(payload);
        assertNotNull(enclaveId);
    }

    @Test
    public void testValidationFailure_ExtraEnvOverride() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Integration, true);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        envOverrides.put("UNEXPECTED", "UNEXPECTED");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(newPayload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_NotConfidentialSpace() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload().toBuilder()
                .swName("dummy")
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void testValidationSuccess_EURegion() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload().toBuilder()
                .gceZone("europe-north1-a")
                .build();
        assertDoesNotThrow(() -> validator.validate(payload));
    }

    @Test
    public void testValidationFailure_NotStableConfidentialSpace() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload().toBuilder()
                .csSupportedAttributes(null)
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_NoRestartPolicy() {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload().toBuilder()
                .restartPolicy("")
                .build();
        var e = assertThrows(AttestationException.class, () -> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException) e).getAttestationFailure());
    }

    @Test
    public void verifySameEnclaveId_DifferentPartner() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload1 = generateBasicPayload();
        var enclaveId1 = validator.validate(payload1);

        var envOverrides2 = new HashMap<>(payload1.getEnvOverrides());
        envOverrides2.put(PolicyValidator.ENV_OPERATOR_API_KEY_SECRET_NAME, "different_api_key");
        var payload2 = payload1.toBuilder()
                .envOverrides(envOverrides2)
                .build();
        var enclaveId2 = validator.validate(payload2);

        assertEquals(enclaveId1, enclaveId2);
    }

    @Test
    public void verifyDifferentEnclaveId_DifferentImageDigest() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload1 = generateBasicPayload();
        var enclaveId1 = validator.validate(payload1);

        var payload2 = payload1.toBuilder()
                .workloadImageDigest("sha256:c5a2c96250612366ea272ffac6d9744aaf4b45aacd96aa7cfcb931ee3b558259")
                .build();
        var enclaveId2 = validator.validate(payload2);

        assertNotEquals(enclaveId1, enclaveId2);
    }

    @Test
    public void verifyDifferentEnclaveId_DebugMode() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload1 = generateBasicPayload();
        var enclaveId1 = validator.validate(payload1);

        var payload2 = payload1.toBuilder()
                .dbgStat("")
                .build();
        var enclaveId2 = validator.validate(payload2);

        assertNotEquals(enclaveId1, enclaveId2);
    }

    @Test
    public void testValidationFailure_DifferentAttestationUrl() {
        var validator = new PolicyValidator("https://someother.uidapi.com", OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload();
        Throwable t = assertThrows(AttestationException.class, () -> validator.validate(payload));
        assertEquals("The given attestation URL is unknown. Given URL: " + ATTESTATION_URL, t.getMessage());
        assertEquals(AttestationFailure.UNKNOWN_ATTESTATION_URL, ((AttestationClientException) t).getAttestationFailure());

    }

    @Test
    public void testValidationSuccess_CoreUrlNotProvided() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, true);
        var payload = generateBasicPayload();
        HashMap<String, String> envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.remove(PolicyValidator.ENV_CORE_ENDPOINT);
        envOverrides.remove(PolicyValidator.ENV_OPT_OUT_ENDPOINT);
        var payload1 = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();

        var enclaveId = validator.validate(payload1);
        assertEquals(2, payload1.getEnvOverrides().size());
        assertNotNull(enclaveId);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("envOverrideValidationCases")
    public void testEnvOverrideValidation(String caseName, Environment coreEnvironment, String coreOptOutUrl, boolean enforce,
                                          String declaredEnvironment, String givenOptOutUrl,
                                          AttestationFailure expectedFailure, String expectedMessage) throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL, coreOptOutUrl, coreEnvironment, enforce);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, declaredEnvironment);
        if (givenOptOutUrl == null) {
            envOverrides.remove(PolicyValidator.ENV_OPT_OUT_ENDPOINT);
        } else {
            envOverrides.put(PolicyValidator.ENV_OPT_OUT_ENDPOINT, givenOptOutUrl);
        }
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();

        if (expectedFailure == null) {
            assertNotNull(validator.validate(newPayload));
        } else {
            var e = assertThrows(AttestationClientException.class, () -> validator.validate(newPayload));
            assertEquals(expectedFailure, e.getAttestationFailure());
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    private static Stream<Arguments> envOverrideValidationCases() {
        var unknownOptOutUrl = AttestationFailure.UNKNOWN_ATTESTATION_URL;
        var badFormat = AttestationFailure.BAD_FORMAT;
        return Stream.of(
                Arguments.of("testValidationSuccess_MatchingOptOutUrl", Environment.Production, OPT_OUT_URL, true, "prod", OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_EquivalentOptOutUrl", Environment.Production, OPT_OUT_URL, true, "prod", "https://OPTOUT.uidapi.com/some/path", null, null),
                Arguments.of("testValidationSuccess_NoOptOutUrl", Environment.Production, OPT_OUT_URL, true, "prod", null, null, null),
                Arguments.of("testValidationSuccess_IntegCoreCustomOptOutUrl", Environment.Integration, OPT_OUT_URL, true, "integ", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_IntegCoreDeclaredProd", Environment.Integration, OPT_OUT_URL, true, "prod", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_CoreEnvironmentUnset", null, OPT_OUT_URL, true, "integ", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_NotEnforced_UnknownOptOutUrl", Environment.Production, OPT_OUT_URL, false, "prod", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_NotEnforced_EnvironmentMismatch", Environment.Production, OPT_OUT_URL, false, "integ", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_NotEnforced_CoreOptOutUrlUnset", Environment.Production, null, false, "prod", OPT_OUT_URL, null, null),
                Arguments.of("testValidationSuccess_IntegCoreOptOutUrlUnset", Environment.Integration, null, true, "integ", OTHER_OPT_OUT_URL, null, null),
                Arguments.of("testValidationFailure_UnknownOptOutUrl", Environment.Production, OPT_OUT_URL, true, "prod", OTHER_OPT_OUT_URL,
                        unknownOptOutUrl, "The given opt-out URL is unknown. Given URL: " + OTHER_OPT_OUT_URL),
                Arguments.of("testValidationFailure_UnknownOptOutUrl_DifferentScheme", Environment.Production, OPT_OUT_URL, true, "prod", "http://optout.uidapi.com",
                        unknownOptOutUrl, "The given opt-out URL is unknown. Given URL: http://optout.uidapi.com"),
                Arguments.of("testValidationFailure_UnknownOptOutUrl_DifferentPort", Environment.Production, OPT_OUT_URL, true, "prod", "https://optout.uidapi.com:8443",
                        unknownOptOutUrl, "The given opt-out URL is unknown. Given URL: https://optout.uidapi.com:8443"),
                Arguments.of("testValidationFailure_UnknownOptOutUrl_NotAUrl", Environment.Production, OPT_OUT_URL, true, "prod", "optout_endpoint",
                        unknownOptOutUrl, "The given opt-out URL is unknown. Given URL: optout_endpoint"),
                Arguments.of("testValidationFailure_EnvironmentMismatch", Environment.Production, OPT_OUT_URL, true, "integ", OPT_OUT_URL,
                        badFormat, "Environment mismatch. Expected prod, given: integ"),
                Arguments.of("testValidationFailure_EnvironmentMismatch_UnknownOptOutUrl", Environment.Production, OPT_OUT_URL, true, "integ", OTHER_OPT_OUT_URL,
                        badFormat, "Environment mismatch. Expected prod, given: integ")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testConstructorFailure_EnforcedProdWithoutCoreOptOutUrl(String coreOptOutUrl) {
        assertThrows(IllegalArgumentException.class, () -> new PolicyValidator(ATTESTATION_URL, coreOptOutUrl, Environment.Production, true));
    }

    @Test
    public void testValidationSuccess_NotEnforced_LogsWarning() throws AttestationException {
        var logger = (Logger) LoggerFactory.getLogger(PolicyValidator.class);
        var memoryAppender = new MemoryAppender();
        memoryAppender.start();
        logger.addAppender(memoryAppender);
        try {
            var validator = new PolicyValidator(ATTESTATION_URL, OPT_OUT_URL, Environment.Production, false);
            var payload = generateBasicPayload();
            var envOverrides = new HashMap<>(payload.getEnvOverrides());
            envOverrides.put(PolicyValidator.ENV_OPT_OUT_ENDPOINT, OTHER_OPT_OUT_URL);
            var newPayload = payload.toBuilder()
                    .envOverrides(envOverrides)
                    .build();

            assertNotNull(validator.validate(newPayload));
            assertTrue(memoryAppender.contains("GCP override check failed (not enforced): The given opt-out URL is unknown. Given URL: " + OTHER_OPT_OUT_URL, Level.WARN));
        } finally {
            logger.detachAppender(memoryAppender);
        }
    }

    private TokenPayload generateBasicPayload() {
        var builder = TokenPayload.builder()
                .gceZone("us-west1-b")
                .swVersion("CONFIDENTIAL_SPACE")
                .dbgStat("disabled-since-boot")
                .swName("CONFIDENTIAL_SPACE")
                .csSupportedAttributes(List.of("LATEST", "STABLE", "USABLE"))
                .workloadImageReference("imageRef")
                .workloadImageDigest("sha256:b5a2c96250612366ea272ffac6d9744aaf4b45aacd96aa7cfcb931ee3b558259")
                .restartPolicy("NEVER")
                .envOverrides(Map.of(
                        PolicyValidator.ENV_ENVIRONMENT, "prod",
                        PolicyValidator.ENV_OPERATOR_API_KEY_SECRET_NAME, "dummy_api_key",
                        PolicyValidator.ENV_CORE_ENDPOINT, ATTESTATION_URL,
                        PolicyValidator.ENV_OPT_OUT_ENDPOINT, OPT_OUT_URL
                ));
        return builder.build();
    }
}
