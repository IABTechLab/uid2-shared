package com.uid2.shared.secret;

import com.uid2.shared.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class KeyHasher {
    private static final int SALT_BYTES = 32;

    public static class KeyHashResult {
        public final String hash;
        public final String salt;

        public KeyHashResult(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }

    private static byte[] generateSaltBytes() {
        final SecureRandom random = new SecureRandom();
        final byte[] bytes = new byte[SALT_BYTES];
        random.nextBytes(bytes);

        return bytes;
    }

    public byte[] hashKey(String key, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            return md.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public KeyHashResult hashKey(String key) {
        byte[] saltBytes = generateSaltBytes();
        byte[] hashBytes = hashKey(key, saltBytes);
        return new KeyHashResult(Utils.toBase64String(hashBytes), Utils.toBase64String(saltBytes));
    }
}
