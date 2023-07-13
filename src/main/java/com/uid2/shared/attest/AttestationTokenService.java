package com.uid2.shared.attest;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class AttestationTokenService implements IAttestationTokenService {

    private final String encryptionKey;
    private final String encryptionSalt;
    private final long expiresAfterSeconds;
    private final ThreadLocalRandom random;
    private final Clock clock;

    @Deprecated
    public AttestationTokenService(String encryptionKey, String encryptionSalt) {
        this(encryptionKey, encryptionSalt, 2 * 3600); // 2 hours by default
    }

    public AttestationTokenService(String encryptionKey, String encryptionSalt, long expiresAfterSeconds) {
        this(encryptionKey, encryptionSalt, expiresAfterSeconds, ThreadLocalRandom.current(), Clock.systemUTC()  );
    }

    public AttestationTokenService(String encryptionKey, String encryptionSalt, long expiresAfterSeconds, ThreadLocalRandom random, Clock clock) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.expiresAfterSeconds = expiresAfterSeconds;
        this.random = random;
        this.clock = clock;
    }

    @Override
    public AttestationToken createToken(String userToken) {
        long randomOffset = this.random.nextLong(300, 600); // random time between 5 and 10 minutes more to create some variation between when operators expire
        Instant expiresAt = this.clock.instant().plus(this.expiresAfterSeconds + randomOffset, ChronoUnit.SECONDS);
        return new AttestationToken(userToken, expiresAt);
    }

    @Override
    public String createEncryptedToken(AttestationToken attToken) {
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
