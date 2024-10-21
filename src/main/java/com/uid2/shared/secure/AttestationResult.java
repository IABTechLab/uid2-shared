package com.uid2.shared.secure;

public class AttestationResult {
    private final AttestationFailure failure;
    private final byte[] publicKey;

    private final String enclaveId;

    private final AttestationClientException attestationClientException;

    public AttestationResult(AttestationFailure reasonToFail) {
        this.failure = reasonToFail;
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationClientException = null;
    }

    public AttestationResult(AttestationClientException exception) {
        this.failure = exception.getAttestationFailure();
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationClientException = exception;
    }

    public AttestationResult(byte[] publicKey, String enclaveId) {
        this.failure = AttestationFailure.NONE;
        this.publicKey = publicKey;
        this.enclaveId = enclaveId;
        this.attestationClientException = null;
    }

    public boolean isSuccess() {
        return failure == AttestationFailure.NONE;
    }

    public AttestationFailure getFailure() { return this.failure; }

    public String getReason() {
        if (this.attestationClientException != null) {
            return this.attestationClientException.getMessage();
        }
        return this.failure.explain();
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getEnclaveId() {
        return enclaveId;
    }
}
