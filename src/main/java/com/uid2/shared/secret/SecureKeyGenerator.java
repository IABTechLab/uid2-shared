package com.uid2.shared.secret;

import com.google.common.primitives.Bytes;
import com.uid2.shared.Utils;
import com.uid2.shared.model.KeyGenerationResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecureKeyGenerator implements IKeyGenerator {
    public SecureKeyGenerator() {
    }

    @Override
    public byte[] generateRandomKey(int keyLen) {
        final SecureRandom random = new SecureRandom();
        final byte[] bytes = new byte[keyLen];
        random.nextBytes(bytes);
        return bytes;
    }

    @Override
    public String generateRandomKeyString(int keyLen) {
        return Utils.toBase64String(generateRandomKey(keyLen));
    }

    @Override
    public KeyGenerationResult generateFormattedKeyStringAndKeyHash(int keyLen) {
        String key = this.generateRandomKeyString(keyLen);
        String formattedKey = key.length() >= 6 ? new StringBuilder(key).insert(6, ".").toString() : key;

        byte[] keyBytes = Utils.decodeBase64String(key);
        byte[] saltBytes = generateRandomKey(keyLen);

        MessageDigest md = createMessageDigest();
        md.update(saltBytes);
        byte[] hashBytes = md.digest(keyBytes); // This will always generate a byte array of length 512 (64 bytes)

        String hash = Utils.toBase64String(hashBytes); // This will always generate a String with 88 chars (86 + 2 padding)
        String salt = Utils.toBase64String(saltBytes);
        String keyHash = String.format("%s$%s", hash, salt);

        return new KeyGenerationResult(formattedKey, keyHash);
    }

    @Override
    public boolean compareFormattedKeyStringAndKeyHash(String formattedKey, String keyHash) {
        String inputKey = formattedKey.substring(formattedKey.lastIndexOf("-") + 1).replace(".", "");
        byte[] inputKeyBytes = Utils.decodeBase64String(inputKey);

        byte[] hashBytes = Utils.decodeBase64String(keyHash.split("\\$")[0]);
        byte[] saltBytes = Utils.decodeBase64String(keyHash.split("\\$")[1]);

        MessageDigest md = createMessageDigest();
        md.update(saltBytes);
        byte[] inputHashBytes = md.digest(inputKeyBytes);

        return Arrays.equals(inputHashBytes, hashBytes);
    }

    private MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
