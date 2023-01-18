package com.uid2.shared.secure;

import com.uid2.shared.attest.AttestationTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class AttestationTokenTest {
    private static final String ENCRYPTION_KEY = "attestation-token-secret";
    private static final String SALT = "attestation-token-salt";
    @Test
    public void testAttestationToken() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = ats.createToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS));
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));
    }

    @Test
    public void testAttestationTokenBadAlgorithm() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = ats.createToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS));
        final String badAttestationToken =
                attestationToken.substring(0, attestationToken.length() - 1) + "q";
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }

    @Test
    public void testAttestationTokenNoAlgorithm() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = ats.createToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS));
        final String badAttestationToken = String.join("-",
                Arrays.copyOfRange(attestationToken.split("-"), 0, 2));
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }

    @Test
    public void testAttestationTokenMalformed() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = ats.createToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS));
        final String badAttestationToken = attestationToken + "-hoho";
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }
}
