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
import io.micrometer.core.instrument.Tag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import java.io.*;
import java.util.List;

public class CloudEncryptionHelpers {
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
            incrementFailureCounter("Key not found", keyId, storeName);
            throw new IllegalStateException(String.format("No matching key found for S3 file decryption - key_id=%d store=%s", keyId, storeName));
        }

        try {
            byte[] secret = Base64.getDecoder().decode(decryptionKey.getSecret());
            byte[] decryptedBytes = AesGcm.decrypt(encryptedPayload, 0, secret);

            incrementSuccessCounter(keyId, storeName);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            incrementFailureCounter("Internal decryption failure", keyId, storeName);
            throw new RuntimeException(String.format("Internal decryption failure - key_id=%d store=%s", keyId, storeName), e);
        }
    }

    private static void incrementSuccessCounter(int keyId, String storeName) {
        incrementCounter(true, null, keyId, storeName);
    }

    private static void incrementFailureCounter(String reason, int keyId, String storeName) {
        incrementCounter(false, reason, keyId, storeName);
    }

    private static void incrementCounter(boolean success, String reason, int keyId, String storeName) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("key_id", String.valueOf(keyId)));
        tags.add(Tag.of("store", storeName));
        if (!success) {
            tags.add(Tag.of("reason", reason));
        }

        Counter.builder(String.format("uid2.cloud_decryption.%s_total", success ? "success" : "failure"))
                .description(String.format("counter for %s decryptions", success ? "successful" : "failed"))
                .tags(tags)
                .register(Metrics.globalRegistry)
                .increment();
    }
}
