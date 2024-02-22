package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyValidatorTest {
    private static byte[] encodeStringUnicodeAttestationEndpoint(String data) {
        // buffer.array() may include extra empty bytes at the end. This returns only the bytes that have data
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(data);
        return Arrays.copyOf(buffer.array(), buffer.limit());
    }
    private static final String PUBLIC_KEY = "public_key";
    private static final String CCE_POLICY_DIGEST = "digest";
    private static final String ATTESTATION_URL = "https://example.com";
    private static final String ENCODED_ATTESTATION_URL = Base64.getEncoder().encodeToString(encodeStringUnicodeAttestationEndpoint(ATTESTATION_URL));

    @Test
    public void testValidationSuccess() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload, PUBLIC_KEY);
        assertEquals(CCE_POLICY_DIGEST, enclaveId);
    }

    @Test
    public void testValidationFailure_VMInfo() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var newPayload = generateBasicPayload()
                .toBuilder()
                .attestationType("dummy")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_UVMInfo() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var newPayload = generateBasicPayload()
                .toBuilder()
                .complianceStatus("dummy")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_VMDebug() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var newPayload = generateBasicPayload()
                .toBuilder()
                .vmDebuggable(true)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_PublicKeyNotMatch() throws AttestationException {
        var newRunTimeData = generateBasicRuntimeData()
                .toBuilder()
                .publicKey("dummy")
                .build();
        var validator = new PolicyValidator(ATTESTATION_URL);
        var newPayload = generateBasicPayload()
                .toBuilder()
                .runtimeData(newRunTimeData)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_LocationNotSupported() throws AttestationException {
        var newRunTimeData = generateBasicRuntimeData()
                .toBuilder()
                .location("West Europe")
                .build();
        var validator = new PolicyValidator(ATTESTATION_URL);
        var newPayload = generateBasicPayload()
                .toBuilder()
                .runtimeData(newRunTimeData)
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    private MaaTokenPayload generateBasicPayload() {
        return MaaTokenPayload.builder()
                .attestationType("sevsnpvm")
                .complianceStatus("azure-compliant-uvm")
                .vmDebuggable(false)
                .runtimeData(generateBasicRuntimeData())
                .ccePolicyDigest(CCE_POLICY_DIGEST)
                .build();
    }

    private RuntimeData generateBasicRuntimeData(){
        return RuntimeData.builder()
                .publicKey(PUBLIC_KEY)
                .attestationUrl(ENCODED_ATTESTATION_URL)
                .location("East US")
                .build();
    }

    @Test
    public void testValidationSuccess_SameAttestationUrl() throws AttestationException {
        var validator = new PolicyValidator(ATTESTATION_URL);
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload, PUBLIC_KEY);
        assertEquals(CCE_POLICY_DIGEST, enclaveId);
    }

    @Test
    public void testValidationFailure_DifferentAttestationUrl() {
        var validator = new PolicyValidator("https://someother.uidapi.com");
        var payload = generateBasicPayload();
        Throwable t = assertThrows(AttestationException.class, ()-> validator.validate(payload, PUBLIC_KEY));
        assertEquals("The given attestation URL is unknown. Given URL: " + ATTESTATION_URL, t.getMessage());
        assertEquals(AttestationFailure.UNKNOWN_ATTESTATION_URL, ((AttestationClientException)t).getAttestationFailure());

    }
}
