package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PolicyValidatorTest {
    @Test
    public void testValicationSuccess_FullProd() throws AttestationException {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload);
    }

    @Test
    public void testValicationFailure_MissRequiredEnvProd() {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.remove(PolicyValidator.ENV_ENVIRONMENT);
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
    }

    @Test
    public void testValicationFailure_UnknownEnv() {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "env1");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
    }

    @Test
    public void testValicationFailure_UnknownScope() {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_IDENTITY_SCOPE, "scope1");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
    }

    @Test
    public void testValicationSuccess_IntegNoOptionalEnv() throws AttestationException {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        payload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var enclaveId = validator.validate(payload);
    }

    @Test
    public void testValicationSuccess_IntegHasOptionalEnv() throws AttestationException {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        envOverrides.put(PolicyValidator.ENV_CORE_ENDPOINT, "coreendpoint");
        payload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        var enclaveId = validator.validate(payload);
    }

    @Test
    public void testValidationFailure_ExtraEnvOverride(){
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var envOverrides = new HashMap<>(payload.getEnvOverrides());
        envOverrides.put(PolicyValidator.ENV_ENVIRONMENT, "integ");
        envOverrides.put("UNEXPECTED", "UNEXPECTED");
        var newPayload = payload.toBuilder()
                .envOverrides(envOverrides)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload));
    }

    @Test
    public void testValidationFailure_NotConfidentialSpace(){
        var validator = new PolicyValidator();
        var payload = generateBasicPayload().toBuilder()
                .swName("dummy")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(payload));
    }

    @Test
    public void testValidationFailure_NotStableConfidentialSpace(){
        var validator = new PolicyValidator();
        var payload = generateBasicPayload().toBuilder()
                .csSupportedAttributes(null)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(payload));
    }

    @Test
    public void testValidationFailure_NoRestartPolicy(){
        var validator = new PolicyValidator();
        var payload = generateBasicPayload().toBuilder()
                .restartPolicy("")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(payload));
    }

    @Test
    public void verifySameEnclaveId_DifferentPartner() throws AttestationException {
        var validator = new PolicyValidator();
        var payload1 = generateBasicPayload();
        var enclaveId1 = validator.validate(payload1);

        var envOverrides2 = new HashMap<>(payload1.getEnvOverrides());
        envOverrides2.put(PolicyValidator.ENV_OPERATOR_API_KEY, "different_api_key");
        var payload2 = payload1.toBuilder()
                .envOverrides(envOverrides2)
                .build();
        var enclaveId2 = validator.validate(payload2);

        assertEquals(enclaveId1, enclaveId2);
    }

    @Test
    public void verifyDifferentEnclaveId_DifferentImageDigest() throws AttestationException {
        var validator = new PolicyValidator();
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
        var validator = new PolicyValidator();
        var payload1 = generateBasicPayload();
        var enclaveId1 = validator.validate(payload1);

        var payload2 = payload1.toBuilder()
                .dbgStat("")
                .build();
        var enclaveId2 = validator.validate(payload2);

        assertNotEquals(enclaveId1, enclaveId2);
    }

    private TokenPayload generateBasicPayload(){
        var builder = TokenPayload.builder()
                .swVersion("CONFIDENTIAL_SPACE")
                .dbgStat("disabled-since-boot")
                .swName("CONFIDENTIAL_SPACE")
                .csSupportedAttributes(List.of("LATEST", "STABLE", "USABLE"))
                .workloadImageReference("imageRef")
                .workloadImageDigest("sha256:b5a2c96250612366ea272ffac6d9744aaf4b45aacd96aa7cfcb931ee3b558259")
                .restartPolicy("NEVER")
                .envOverrides(Map.of(
                        PolicyValidator.ENV_ENVIRONMENT, "prod",
                        PolicyValidator.ENV_IDENTITY_SCOPE, "uid2",
                        PolicyValidator.ENV_OPERATOR_API_KEY, "dummy_api_key"
                ));
        return builder.build();
    }
}
