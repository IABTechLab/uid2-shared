package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.KeysetKey;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AesGcmTest {
    @Test
    public void testEncryptionDecryption() {
        final KeysetKey key = new KeysetKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), 123);
        String plaintxt = "hello world";
        EncryptedPayload payload = AesGcm.encrypt(plaintxt.getBytes(StandardCharsets.UTF_8), key);
        String decryptedText = new String(AesGcm.decrypt(payload.getPayload(), 0, key), StandardCharsets.UTF_8);
        assertEquals(plaintxt, decryptedText);
    }

    @Test
    public void testEncryptionDecryptionString() {
        final KeysetKey key = new KeysetKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), 123);
        String plaintxt = "hello world";
        EncryptedPayload payload = AesGcm.encrypt(plaintxt, key);
        String decryptedText = new String(AesGcm.decrypt(payload.getPayload(), 0, key), StandardCharsets.UTF_8);
        assertEquals(plaintxt, decryptedText);
    }

    @Test
    public void testDecryption() {
        byte[] keyBytes = new byte[]{19, -105, -79, 100, 11, 38, -93, 100, 123, 111, 68, -57, 67, -12, -33, 14, -53, -120, -66, -116, 30, 9, -123, -50, -36, 73, 79, 85, 78, -18, 42, -108};
        byte[] encrypted = new byte[]{-2, 35, -109, -1, -123, 75, 111, 93, 83, 0, -11, -97, -69, -10, 88, 82, -62, 83, 76, -75, -51, 45, 87, 102, 38, 117, 98, 84, -78, -100, -2, -99, -3, -121, 94, 23, 75, 84, 20};
        final KeysetKey key = new KeysetKey(1, keyBytes, Instant.now(), Instant.now(), Instant.now(), 123);
        byte[] payload = AesGcm.decrypt(encrypted,0, key);
        String expected = "hello world";
        String results = new String(payload);
        assertEquals(results, expected);
    }
}
