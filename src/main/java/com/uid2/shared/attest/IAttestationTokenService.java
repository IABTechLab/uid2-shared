package com.uid2.shared.attest;

import java.time.Instant;

public interface IAttestationTokenService {
    /**
     * Create attestation token from user token
     * @param userToken
     * @return attestation token
     */
    AttestationToken createToken(String userToken);

    /**
     * Create encrypted token from AttestationToken
     * @param attToken
     * @return encrypted attestation token
     */
    public String createEncryptedToken(AttestationToken attToken);

    /**
     * Create attestation token from user token
     * @param userToken
     * @param expiresAt
     * @return
     */
    @Deprecated
    String createToken(String userToken, Instant expiresAt);

    /**
     * Validate if attestation is generated from the user token provided
     * @param userToken
     * @param attestationToken
     * @return if the credential matches
     */
    boolean validateToken(String userToken, String attestationToken);
}
