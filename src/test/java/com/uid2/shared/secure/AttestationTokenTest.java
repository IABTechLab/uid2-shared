package com.uid2.shared.secure;

import com.uid2.shared.attest.AttestationToken;
import com.uid2.shared.attest.AttestationTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AttestationTokenTest {
    private static final String ENCRYPTION_KEY = "attestation-token-secret";
    private static final String SALT = "attestation-token-salt";

    private final Clock clock = mock(Clock.class);

    private final ThreadLocalRandom random = mock(ThreadLocalRandom.class);

    @Test
    public void testAttestationToken() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT, 3600);
        final String attestationToken = ats.createToken(
                "userToken");
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));
    }

    @Test
    public void testAttestationTokenBadAlgorithm() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT, 3600);
        final String attestationToken = ats.createToken(
                "userToken");
        final String badAttestationToken =
                attestationToken.substring(0, attestationToken.length() - 1) + "q";
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }

    @Test
    public void testAttestationTokenNoAlgorithm() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT, 3600);
        final String attestationToken = ats.createToken(
                "userToken");
        final String badAttestationToken = String.join("-",
                Arrays.copyOfRange(attestationToken.split("-"), 0, 2));
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }

    @Test
    public void testAttestationTokenMalformed() {
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT, 3600);
        final String attestationToken = ats.createToken(
                "userToken");
        final String badAttestationToken = attestationToken + "-hoho";
        Assertions.assertFalse(ats.validateToken("userToken", badAttestationToken));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 100L, 234L, 599L})
    public void testAttestationTokenSettingLifetimeHasExpectedPlusRandomValue(long randomValue) {
        final long lifetime = 1800;
        final Instant fixedInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(fixedInstant);
        when(random.nextLong(300, 600)).thenReturn(randomValue);
        final Instant targetLifetime = fixedInstant.plus(1800 + randomValue, ChronoUnit.SECONDS);
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT, lifetime, random, clock);

        final String attestationToken = ats.createToken("userToken");
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));

        final AttestationToken reconstructToken = AttestationToken.fromEncrypted(attestationToken, ENCRYPTION_KEY, SALT);
        assertThat(reconstructToken.getExpiresAt()).isEqualTo(targetLifetime);
    }
    @Test
    public void testAttestationTokenDefaultLifetimeHasDefaultPlusRandomValue() {
        final long defaultLifetime = 7200;

        // using the default constructor requires the use of a range, rather than a specific value
        final Instant expiryLowerBound = Instant.now().plusSeconds(defaultLifetime + (5 * 60) + 1); // bounds are exclusive
        final Instant expiryUpperBound = Instant.now().plusSeconds(defaultLifetime + (10 * 60) - 1);
        final AttestationTokenService ats = new AttestationTokenService(ENCRYPTION_KEY, SALT); // until the default constructor is removed, this must use it for the test to be valid

        final String attestationToken = ats.createToken("userToken");
        Assertions.assertTrue(ats.validateToken("userToken", attestationToken));

        final AttestationToken reconstructToken = AttestationToken.fromEncrypted(attestationToken, ENCRYPTION_KEY, SALT);
        assertThat(reconstructToken.getExpiresAt()).isBetween(expiryLowerBound, expiryUpperBound);
    }

    @Test
    public void testAttestationTokenExpiry() {
        Assertions.assertFalse(new AttestationToken(
                "hello",
                Instant.now().minusSeconds(1)).validate("hello"));
    }
}
