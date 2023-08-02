package com.uid2.shared.encryption;

import com.uid2.shared.model.EncryptedPayload;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.model.KeyIdentifier;
import com.uid2.shared.model.KeysetKey;
import io.vertx.core.buffer.Buffer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AesGcm {
    private static final String cipherScheme = "AES/GCM/NoPadding";
    public static final int GCM_AUTHTAG_LENGTH = 16;
    public static final int GCM_IV_LENGTH = 12;

    public static EncryptedPayload encrypt(byte[] b, KeysetKey key) {
        return encrypt(b, key.getKeyBytes(), key.getKeyIdentifier());
    }

    public static EncryptedPayload encrypt(String s, KeysetKey key) {
        return encrypt(s.getBytes(StandardCharsets.UTF_8), key.getKeyBytes(), key.getKeyIdentifier());
    }

    private static EncryptedPayload encrypt(byte[] b, byte[] secretBytes, KeyIdentifier keyIdentifier) {
        return new EncryptedPayload(keyIdentifier, encrypt(b, secretBytes));
    }

    public static byte[] encrypt(byte[] b, byte[] secretBytes) {
        try {
            final SecretKey k = new SecretKeySpec(secretBytes, "AES");
            final Cipher c = Cipher.getInstance(cipherScheme);
            final byte[] ivBytes = Random.getBytes(GCM_IV_LENGTH);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTHTAG_LENGTH * 8, ivBytes);
            c.init(Cipher.ENCRYPT_MODE, k, gcmParameterSpec);
            return Buffer.buffer().appendBytes(ivBytes).appendBytes(c.doFinal(b)).getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Unable to Encrypt", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedBytes, int offset, KeysetKey key) {
        return decrypt(encryptedBytes, offset, key.getKeyBytes());
    }

    public static byte[] decrypt(byte[] encryptedBytes, int offset, byte[] secretBytes) {
        try {
            final SecretKey key = new SecretKeySpec(secretBytes, "AES");
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTHTAG_LENGTH * 8, encryptedBytes, offset, GCM_IV_LENGTH);
            final Cipher c = Cipher.getInstance(cipherScheme);
            c.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            return c.doFinal(encryptedBytes, offset + GCM_IV_LENGTH, encryptedBytes.length - offset - GCM_IV_LENGTH);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Decrypt", e);
        }
    }

    public static EncryptedPayload encrypt(byte[] b, EncryptionKey key) {
        return encrypt(b, key.getKeyBytes(), key.getKeyIdentifier());
    }

    public static EncryptedPayload encrypt(String s, EncryptionKey key) {
        return encrypt(s.getBytes(StandardCharsets.UTF_8), key.getKeyBytes(), key.getKeyIdentifier());
    }

    public static byte[] decrypt(byte[] encryptedBytes, int offset, EncryptionKey key) {
        return decrypt(encryptedBytes, offset, key.getKeyBytes());
    }
}
