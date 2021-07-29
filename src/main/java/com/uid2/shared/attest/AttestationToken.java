// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

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
