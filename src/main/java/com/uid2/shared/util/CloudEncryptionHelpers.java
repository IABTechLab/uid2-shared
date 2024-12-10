package com.uid2.shared.util;

import java.io.InputStream;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;

import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.*;

public class CloudEncryptionHelpers {
    public static String decryptInputStream(InputStream inputStream, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) throws IOException {
        String encryptedContent = inputStreamToString(inputStream);
        JsonObject json = new JsonObject(encryptedContent);
        int keyId = json.getInteger("key_id");
        String encryptedPayload = json.getString("encrypted_payload");
        CloudEncryptionKey decryptionKey = cloudEncryptionKeyProvider.getKey(keyId);

        if (decryptionKey == null) {
            throw new IllegalStateException("No matching S3 key found for decryption for key ID: " + keyId);
        }

        byte[] secret = Base64.getDecoder().decode(decryptionKey.getSecret());
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPayload);
        byte[] decryptedBytes = AesGcm.decrypt(encryptedBytes, 0, secret);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }
}
