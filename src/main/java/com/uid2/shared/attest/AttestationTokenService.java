package com.uid2.shared.attest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AttestationTokenService implements IAttestationTokenService {

    private final String encryptionKey;
    private final String encryptionSalt;
    private final long expiresAfterSeconds;

    @Deprecated
    public AttestationTokenService(String encryptionKey, String encryptionSalt) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.expiresAfterSeconds = 2 * 3600; // 2 hours;
    }

    public AttestationTokenService(String encryptionKey, String encryptionSalt, long expiresAfterSeconds) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.expiresAfterSeconds = expiresAfterSeconds;
    }

    @Override
    public String createToken(String userToken) {
        Instant expiresAt = Instant.now().plus(this.expiresAfterSeconds, ChronoUnit.SECONDS);
        AttestationToken attToken = new AttestationToken(userToken, expiresAt);
        return attToken.encode(encryptionKey, encryptionSalt);
    }

    @Deprecated
    @Override
    public String createToken(String userToken, Instant expiresAt) {
        AttestationToken attToken = new AttestationToken(userToken, expiresAt);
        return attToken.encode(encryptionKey, encryptionSalt);
    }

    @Override
    public boolean validateToken(String userToken, String attestationToken) {
        AttestationToken decrypted = AttestationToken.fromEncrypted(
            attestationToken,
            encryptionKey,
            encryptionSalt);
        return decrypted.validate(userToken);
    }
}
