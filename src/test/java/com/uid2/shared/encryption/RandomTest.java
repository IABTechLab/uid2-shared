package com.uid2.shared.encryption;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class RandomTest {
    @Test
    public void testRandomBytes() throws NoSuchAlgorithmException {
        byte[] r1 = Random.getBytes(16);
        byte[] r2 = Random.getBytes(16);
        assertNotSame(r1, r2);
    }

    @Test
    public void testGetRandomKey() {
        byte[] key1 = Random.getRandomKeyBytes();
        byte[] key2 = Random.getRandomKeyBytes();

        assertEquals(key1.length, 32);
        assertNotSame(key1, key2);
    }
}
