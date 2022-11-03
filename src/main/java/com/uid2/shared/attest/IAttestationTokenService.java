package com.uid2.shared.attest;

import java.time.Instant;

public interface IAttestationTokenService {
    String createToken(String userToken, Instant expiresAt, String key, String salt);
    boolean validateToken(String userToken, String attestationToken);
}
