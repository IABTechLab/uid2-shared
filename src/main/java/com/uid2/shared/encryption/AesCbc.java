package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.KeysetKey;
import io.vertx.core.buffer.Buffer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AesCbc {
    private static final String cipherScheme = "AES/CBC/PKCS5Padding";

    public static EncryptedPayload encrypt(byte[] b, KeysetKey key) {
        try {
            final SecretKey k = new SecretKeySpec(key.getKeyBytes(), "AES");
            final Cipher c = Cipher.getInstance(cipherScheme);
            final byte[] ivBytes = Random.getBytes(16);
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            c.init(Cipher.ENCRYPT_MODE, k, ivParameterSpec);
            final byte[] encryptedBytes = c.doFinal(b);
            return new EncryptedPayload(key.getKeyIdentifier(), Buffer.buffer().appendBytes(ivBytes).appendBytes(encryptedBytes).getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Unable to Encrypt", e);
        }
    }

    public static EncryptedPayload encrypt(String s, KeysetKey key) {
        try {
            return encrypt(s.getBytes(StandardCharsets.UTF_8), key);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Encrypt", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedBytes, KeysetKey key) {
        try {
            final SecretKey k = new SecretKeySpec(key.getKeyBytes(), "AES");
            return decrypt(encryptedBytes, k);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Decrypt", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedBytes, SecretKey key) {
        try {
            final IvParameterSpec iv = new IvParameterSpec(encryptedBytes, 0, 16);
            final Cipher c = Cipher.getInstance(cipherScheme);
            c.init(Cipher.DECRYPT_MODE, key, iv);
            return c.doFinal(encryptedBytes, 16, encryptedBytes.length - 16);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Decrypt", e);
        }
    }
}
