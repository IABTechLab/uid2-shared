package com.uid2.shared.attest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class AttestationTokenService implements IAttestationTokenService {

    private final String encryptionKey;
    private final String encryptionSalt;
    private final long expiresAfterSeconds;

    @Deprecated
    public AttestationTokenService(String encryptionKey, String encryptionSalt) {
        this(encryptionKey, encryptionSalt, 2 * 3600);
    }

    public AttestationTokenService(String encryptionKey, String encryptionSalt, long expiresAfterSeconds) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.expiresAfterSeconds = expiresAfterSeconds;
    }

    @Override
    public String createToken(String userToken) {
        long ran = ThreadLocalRandom.current().nextLong(300, 600); // random time between 5 and 10 minutes more to create some variation between when operators expire
        Instant expiresAt = Instant.now().plus(this.expiresAfterSeconds + ran, ChronoUnit.SECONDS);
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
