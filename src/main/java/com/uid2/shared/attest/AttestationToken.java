package com.uid2.shared.attest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

public class AttestationToken {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttestationToken.class);
    private static final String Algorithm = "AES/CBC/PKCS5Padding";

    private final String userToken;
    private final long expiresAt;
    private final long nonce;
    private final boolean isValid;

    public AttestationToken(String plaintext, Instant expiresAt) {
        this(plaintext, expiresAt.getEpochSecond(), generateNonce(), true);
    }

    private AttestationToken(String userToken, long expiresAt, long nonce, boolean isValid) {
        this.userToken = userToken;
        this.expiresAt = expiresAt;
        this.nonce = nonce;
        this.isValid = isValid;
    }

    public static AttestationToken fromPlaintext(String plaintext) {
        try {
            String[] parts = plaintext.split(",");
            return new AttestationToken(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), true);
        } catch (Exception e) {
            LOGGER.info("failed to decode attestation token: {}", e.getMessage());
            return AttestationToken.Failed();
        }
    }

    public static AttestationToken fromEncrypted(String encryptedToken, String paraphrase, String salt) {
        try {
            String[] parts = encryptedToken.split("-");
            if(parts.length != 2) {
                throw new Exception("token must satisfy format: <Base64>-<Base64>");
            }

            byte[] cipherText = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(Cipher.DECRYPT_MODE, getKeyFromPassword(paraphrase, salt), new IvParameterSpec(iv));
            byte[] plaintext = cipher.doFinal(cipherText);
            return fromPlaintext(new String(plaintext));
        } catch (Exception e) {
            LOGGER.debug("failed to decrypt attestation token: {}", e.getMessage());
            return AttestationToken.Failed();
        }
    }

    public String encode(String paraphrase, String salt) {
        try {
            IvParameterSpec iv = generateIv();
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromPassword(paraphrase, salt), iv);
            byte[] cipherText = cipher.doFinal(this.getPlaintext().getBytes());
            return String.format("%s-%s",
                    Base64.getEncoder().encodeToString(cipherText),
                    Base64.getEncoder().encodeToString(iv.getIV()));
        } catch (Exception e) {
            LOGGER.warn("error while encrypting with AES algorithm: " + e.getMessage());
        }
        return null;
    }

    public boolean validate(String userToken) {
        return this.isValid && this.userToken.equals(userToken) && this.expiresAt > Instant.now().getEpochSecond();
    }

    private static SecretKey getKeyFromPassword(String paraphrase, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(paraphrase.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }

    private static long generateNonce() {
        return new Random().nextLong();
    }

    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    private String getPlaintext() {
        return String.format("%s,%d,%d", userToken, expiresAt, nonce);
    }

    private static AttestationToken Failed() {
        return new AttestationToken("invalid", 0L, 0L, false);
    }

    @Override
    public String toString() {
        return "AttestationToken{" +
                "userToken=" + userToken +
                ", expiresAt=" + expiresAt +
                ", nonce=" + nonce +
                '}';
    }

}
