package com.uid2.shared.util;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;

import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.*;

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
            throw new IllegalStateException(String.format("No matching S3 key found for decryption - key_id=%d store=%s", keyId, storeName));
        }

        byte[] secret = Base64.getDecoder().decode(decryptionKey.getSecret());
        byte[] encryptedBytes = encryptedPayload;

        try {
            byte[] decryptedBytes = AesGcm.decrypt(encryptedBytes, 0, secret);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to Decrypt - key_id=%d store=%s", keyId, storeName), e);
        }
    }
}
