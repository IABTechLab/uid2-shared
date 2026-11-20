package com.uid2.shared.secure;

import java.util.Collections;
import java.util.Map;

public class AttestationResult {
    private final AttestationFailure failure;
    private final byte[] publicKey;

    private final String enclaveId;

    private final AttestationClientException attestationClientException;

    private final Map<String, String> attestationDetails;

    public AttestationResult(AttestationFailure reasonToFail) {
        this.failure = reasonToFail;
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationClientException = null;
        this.attestationDetails = Collections.emptyMap();
    }

    public AttestationResult(AttestationClientException exception) {
        this.failure = exception.getAttestationFailure();
        this.publicKey = null;
        this.enclaveId = "Failed attestation, enclave Id unknown";
        this.attestationClientException = exception;
        this.attestationDetails = Collections.emptyMap();
    }

    public AttestationResult(byte[] publicKey, String enclaveId) {
        this.failure = AttestationFailure.NONE;
        this.publicKey = publicKey;
        this.enclaveId = enclaveId;
        this.attestationClientException = null;
        this.attestationDetails = Collections.emptyMap();
    }

    private AttestationResult(AttestationResult result, Map<String, String> attestationDetails) {
        this.failure = result.failure;
        this.publicKey = result.publicKey;
        this.enclaveId = result.enclaveId;
        this.attestationClientException = result.attestationClientException;
        this.attestationDetails = Collections.unmodifiableMap(attestationDetails);
    }

    public AttestationResult withDetails(Map<String, String> attestationDetails) {
        return new AttestationResult(this, attestationDetails);
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

    public Map<String, String> getAttestationDetails() {
        return attestationDetails;
    }
}
