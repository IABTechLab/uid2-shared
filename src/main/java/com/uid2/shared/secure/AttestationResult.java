package com.uid2.shared.secure;

public class AttestationResult {
    private final AttestationFailure failure;
    private final byte[] publicKey;

    public AttestationResult(AttestationFailure reasonToFail) {
        this.failure = reasonToFail;
        this.publicKey = null;
    }

    public AttestationResult(byte[] publicKey) {
        this.failure = AttestationFailure.NONE;
        this.publicKey = publicKey;
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
}
