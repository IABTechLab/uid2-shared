package com.uid2.shared.secure;

import com.uid2.shared.attest.AttestationToken;
import com.uid2.shared.attest.AttestationTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AttestationTokenTest {
    private static final String ENCRYPTION_KEY = "attestation-token-secret";
    private static final String SALT = "attestation-token-salt";
    @Test
    public void testAttestationTokenPlain() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = ats.createToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS));
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));
    }

    @Test
    public void testAttestationTokenBackwardsCompatible() {
        // this test should deprecate together with old encode methods
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = new AttestationToken(
                    "userToken",
                    Instant.now().plus(1, ChronoUnit.HOURS)
                    ).encode(ENCRYPTION_KEY, SALT);
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));
    }

    @Test
    public void testAttestationTokenNew() {
        // this test should deprecate together with old encode methods
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT);
        final String attestationToken = new AttestationToken(
                "userToken",
                Instant.now().plus(1, ChronoUnit.HOURS)
        ).encodeNew(ENCRYPTION_KEY, SALT);
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));
    }
}
