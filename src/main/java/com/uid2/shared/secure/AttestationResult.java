package com.uid2.shared.secure;

public class AttestationResult {
    private final AttestationFailure failure;
    private final byte[] publicKey;

    private final String enclaveId;

    private final AttestationClientException attestationException;

    public AttestationResult(AttestationFailure reasonToFail) {
        this.failure = reasonToFail;
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationException = null;
    }

    public AttestationResult(AttestationClientException exception) {
        this.failure = AttestationFailure.OTHER;
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationException = exception;
    }

    public AttestationResult(byte[] publicKey, String enclaveId) {
        this.failure = AttestationFailure.NONE;
        this.publicKey = publicKey;
        this.enclaveId = enclaveId;
        this.attestationException = null;
    }

    public boolean isSuccess() {
        return failure == AttestationFailure.NONE;
    }

    public AttestationFailure getFailure() { return this.failure; }

    public String getReason() {
        if(this.attestationException != null){
            return this.attestationException.getMessage();
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
