package com.uid2.shared.attest;

import java.time.Instant;

public class EncryptedAttestationToken {
    private final String encodedAttestationToken;
    private final Instant expiresAt;

    public EncryptedAttestationToken(String encodedAttestationToken, Instant expiresAt) {
        this.encodedAttestationToken = encodedAttestationToken;
        this.expiresAt = expiresAt;
    }

    public String getEncodedAttestationToken() {
        return this.encodedAttestationToken;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }
}
