package com.uid2.shared.encryption;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Random {
    private static final ThreadLocal<SecureRandom> threadLocalSecureRandom = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    });

    public static byte[] getBytes(int size) {
        final byte[] bytes = new byte[size];
        threadLocalSecureRandom.get().nextBytes(bytes);
        return bytes;
    }

    public static byte[] getRandomKeyBytes() {
        try {
            final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Trouble Generating Random Key Bytes", e);
        }
    }
}
