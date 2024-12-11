package com.uid2.shared.util;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;

import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.*;

public class CloudEncryptionHelpers {
    public static String decryptInputStream(InputStream inputStream, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) throws IOException {
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
            throw new IllegalStateException("failed to parse json");
        }

        CloudEncryptionKey decryptionKey = cloudEncryptionKeyProvider.getKey(keyId);

        if (decryptionKey == null) {
            throw new IllegalStateException("No matching S3 key found for decryption for key ID: " + keyId);
        }

        byte[] secret = Base64.getDecoder().decode(decryptionKey.getSecret());
        byte[] encryptedBytes = encryptedPayload;
        byte[] decryptedBytes = AesGcm.decrypt(encryptedBytes, 0, secret);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
