package com.uid2.shared.secret;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class KeyHasherTest {
    @Test
    public void HashKey_ReturnsKnownHash_WithGivenSalt() {
        KeyHasher hasher = new KeyHasher();
        byte[] hashedBytes = hasher.hashKey("test-key", "test-salt".getBytes(StandardCharsets.UTF_8));
        assertEquals("hzXFALLdI9ji4ajnzhWdbEQNci+kAoA40Ie6X7bEyjIvMFbhQfYZC1sTPeK+14QM+Ox2a6wJ0U2fLzqnoUgCbQ==", Base64.getEncoder().encodeToString(hashedBytes));
    }

    @Test
    public void HashKey_ReturnsNewHashEverytime_WithRandomSalt() {
        KeyHasher hasher = new KeyHasher();
        KeyHashResult result1 = hasher.hashKey("test-key");
        KeyHashResult result2 = hasher.hashKey("test-key");

        assertNotEquals(result1.hash, result2.hash);
        assertNotEquals(result1.salt, result2.salt);
    }
}
