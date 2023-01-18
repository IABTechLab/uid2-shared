package com.uid2.shared.attest;

import java.time.Instant;

public class AttestationTokenService implements IAttestationTokenService {

    private final String encryptionKey;
    private final String encryptionSalt;

    public AttestationTokenService(String encryptionKey, String encryptionSalt) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
    }

    public String createToken(String userToken, Instant expiresAt) {
        AttestationToken attToken = new AttestationToken(userToken, expiresAt);
        return attToken.encode(encryptionKey, encryptionSalt);
    }

    public boolean validateToken(String userToken, String attestationToken) {
        AttestationToken decrypted = AttestationToken.fromEncrypted(
            attestationToken,
            encryptionKey,
            encryptionSalt);
        return decrypted.validate(userToken);
    }
}
