package com.uid2.shared.encryption;

import junit.framework.TestCase;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTest extends TestCase {
    public void testRandomBytes() throws NoSuchAlgorithmException {
        byte[] r1 = Random.getBytes(16);
        byte[] r2 = Random.getBytes(16);
        assertNotSame(r1, r2);
    }

    public void testGetRandomKey() {
        byte[] key1 = Random.getRandomKeyBytes();
        byte[] key2 = Random.getRandomKeyBytes();

        assertEquals(key1.length, 32);
        assertNotSame(key1, key2);
    }
}
