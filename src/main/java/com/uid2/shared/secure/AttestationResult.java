package com.uid2.shared.secure;

public class AttestationResult {
    private final AttestationFailure failure;
    private final byte[] publicKey;

    private final String enclaveId;

    public AttestationResult(AttestationFailure reasonToFail) {
        this.failure = reasonToFail;
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
    }

    public AttestationResult(byte[] publicKey, String enclaveId) {
        this.failure = AttestationFailure.NONE;
        this.publicKey = publicKey;
        this.enclaveId = enclaveId;
    }

    public boolean isSuccess() {
        return failure == AttestationFailure.NONE;
    }

    public AttestationFailure getFailure() { return this.failure; }

    public String getReason() {
        return this.failure.explain();
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getEnclaveId() {
        return enclaveId;
    }
}
