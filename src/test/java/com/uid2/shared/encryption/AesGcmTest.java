package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.EncryptionKey;
import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class AesGcmTest extends TestCase {
    public void testEncryptionDecryption() {
        final EncryptionKey key = new EncryptionKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), -1);
        String plaintxt = "hello world";
        EncryptedPayload payload = AesGcm.encrypt(plaintxt.getBytes(StandardCharsets.UTF_8), key);
        String decryptedText = new String(AesGcm.decrypt(payload.getPayload(), 0, key), StandardCharsets.UTF_8);
        assertEquals(plaintxt, decryptedText);
    }

    public void testEncryptionString() {
        final EncryptionKey key = new EncryptionKey(1, Random.getRandomKeyBytes(), Instant.now(), Instant.now(), Instant.now(), -1);
        String plaintxt = "hello world";
        EncryptedPayload payload = AesGcm.encrypt(plaintxt, key);
        String decryptedText = new String(AesGcm.decrypt(payload.getPayload(), 0, key), StandardCharsets.UTF_8);
        assertEquals(plaintxt, decryptedText);
    }
}
