package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyValidatorTest {
    private static final String ATTESTATION_URL = "https://core.uidapi.com";
    @Test
    public void testValidationSuccess_FullProd() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload);
        assertNotNull(enclaveId);
    }

    @Test
    public void testValidationFailure_MissRequiredEnvProd() {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.remove(PolicyValidator.ENV_ENVIRONMENT);
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
        assertThat(e, instanceOf(AttestationClientException.class));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_UnknownEnv() {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "env1");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationSuccess_IntegNoOptionalEnv() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
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
    public void testValidationFailure_ExtraEnvOverride(){
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        envOverrides.put("UNEXPECTED", "UNEXPECTED");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_NotConfidentialSpace(){
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload().toBuilder()
                .swName("dummy")
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_EURegion(){
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload().toBuilder()
                .gceZone("europe-north1-a")
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_NotStableConfidentialSpace(){
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload().toBuilder()
                .csSupportedAttributes(null)
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void testValidationFailure_NoRestartPolicy(){
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload().toBuilder()
                .restartPolicy("")
                .build();
        var e = assertThrows(AttestationException.class, ()-> validator.validate(payload));
        assertEquals(AttestationFailure.BAD_FORMAT, ((AttestationClientException)e).getAttestationFailure());
    }

    @Test
    public void verifySameEnclaveId_DifferentPartner() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
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
        var validator = new PolicyValidator(ATTESTATION_URL);
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
        var validator = new PolicyValidator(ATTESTATION_URL);
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
        var validator = new PolicyValidator("https://someother.uidapi.com");
        var payload = generateBasicPayload();
        Throwable t = assertThrows(AttestationException.class, ()-> validator.validate(payload));
        assertEquals("The given attestation URL is unknown. Given URL: " + ATTESTATION_URL, t.getMessage());
        assertEquals(AttestationFailure.UNKNOWN_ATTESTATION_URL, ((AttestationClientException)t).getAttestationFailure());

    }

    @Test
    public void testValidationSuccess_CoreUrlNotProvided() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
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

    private TokenPayload generateBasicPayload(){
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
                        PolicyValidator.ENV_OPT_OUT_ENDPOINT, "optout_endpoint"
                ));
        return builder.build();
    }
}
