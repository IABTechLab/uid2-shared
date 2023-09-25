package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyValidatorTest {
    private static final String PUBLIC_KEY = "abc";
    private static final String CCE_POLICY = "policy";

    @Test
    public void testValidationSuccess() throws AttestationException {
        var validator = new PolicyValidator();
        var payload = generateBasicPayload();
        var enclaveId = validator.validate(payload, PUBLIC_KEY);
        assertEquals(CCE_POLICY, enclaveId);
    }

    @Test
    public void testValidationFailure_VMInfo() throws AttestationException {
        var validator = new PolicyValidator();
        var newPayload = generateBasicPayload()
                .toBuilder()
                .attestationType("dummy")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_UVMInfo() throws AttestationException {
        var validator = new PolicyValidator();
        var newPayload = generateBasicPayload()
                .toBuilder()
                .complianceStatus("dummy")
                .build();
        assertThrows(AttestationException.class, ()-> validator.validate(newPayload, PUBLIC_KEY));
    }

    @Test
    public void testValidationFailure_VMDebug() throws AttestationException {
        var validator = new PolicyValidator();
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
        var validator = new PolicyValidator();
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
        var validator = new PolicyValidator();
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
                .ccePolicy(CCE_POLICY)
                .build();
    }

    private RuntimeData generateBasicRuntimeData(){
        return RuntimeData.builder()
                .publicKey(PUBLIC_KEY)
                .location("East US")
                .build();
    }
}