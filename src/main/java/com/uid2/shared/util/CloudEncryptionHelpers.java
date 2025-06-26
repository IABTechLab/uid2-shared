package com.uid2.shared.util;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;

import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.*;

public class CloudEncryptionHelpers {
    public enum DecryptionStatus {
        SUCCESS,
        KEY_NOT_FOUND,
        INTERNAL_DECRYPTION_FAILURE
    }

    public static String decryptInputStream(InputStream inputStream, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider, String storeName) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(inputStream);
        int keyId = -1;
        byte[] encryptedPayload = null;
        parser.nextToken();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            if(fieldName.equals("key_id")) {
                parser.nextToken();
                keyId = parser.getIntValue();
            }
            if(fieldName.equals("encrypted_payload")) {
                parser.nextToken();
                encryptedPayload = parser.getBinaryValue();
            }
        }

        if(keyId == -1 || encryptedPayload == null) {
            throw new IllegalStateException("Failed to parse JSON");
        }

        CloudEncryptionKey decryptionKey = cloudEncryptionKeyProvider.getKey(keyId);

        if (decryptionKey == null) {
            incrementCounter(DecryptionStatus.KEY_NOT_FOUND, keyId, storeName);
            throw new IllegalStateException(String.format("No matching key found for S3 file decryption - key_id=%d store=%s", keyId, storeName));
        }

        try {
            byte[] secret = Base64.getDecoder().decode(decryptionKey.getSecret());
            byte[] decryptedBytes = AesGcm.decrypt(encryptedPayload, 0, secret);

            incrementCounter(DecryptionStatus.SUCCESS, keyId, storeName);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            incrementCounter(DecryptionStatus.INTERNAL_DECRYPTION_FAILURE, keyId, storeName);
            throw new RuntimeException(String.format("Internal decryption failure - key_id=%d store=%s", keyId, storeName), e);
        }
    }

    private static void incrementCounter(DecryptionStatus status, int keyId, String storeName) {
        Counter.builder("uid2_cloud_decryption_runs_total")
                .description("counter for S3 file decryptions")
                .tag("key_id", String.valueOf(keyId))
                .tag("store", storeName)
                .tag("status", status.toString())
                .register(Metrics.globalRegistry)
                .increment();
    }
}
