package com.uid2.shared.store;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.StoreScope;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import com.uid2.shared.encryption.AesGcm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class EncryptedScopedStoreReader<T> extends ScopedStoreReader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedScopedStoreReader.class);

    private final RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider;

    public EncryptedScopedStoreReader(DownloadCloudStorage fileStreamProvider, StoreScope scope, Parser<T> parser, String dataTypeName, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) {
        super(fileStreamProvider, scope, parser, dataTypeName);
        this.cloudEncryptionKeyProvider = cloudEncryptionKeyProvider;
    }

    @Override
    protected long loadContent(String path) throws Exception {
        try (InputStream inputStream = this.contentStreamProvider.download(path)) {
            String encryptedContent = inputStreamToString(inputStream);
            String decryptedContent = getDecryptedContent(encryptedContent);
            ParsingResult<T> parsed = this.parser.deserialize(new ByteArrayInputStream(decryptedContent.getBytes(StandardCharsets.UTF_8)));
            latestSnapshot.set(parsed.getData());

            final int count = parsed.getCount();
            latestEntryCount.set(count);
            LOGGER.info(String.format("Loaded %d %s", count, dataTypeName));
            return count;
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to load %s", dataTypeName));
            throw e;
        }
    }

    protected String getDecryptedContent(String encryptedContent) throws Exception {
        JsonObject json = new JsonObject(encryptedContent);
        int keyId = json.getInteger("key_id");
        String encryptedPayload = json.getString("encrypted_payload");
        Map<Integer, CloudEncryptionKey> cloudEncryptionKeys = cloudEncryptionKeyProvider.getAll();
        CloudEncryptionKey decryptionKey = null;
        for (CloudEncryptionKey key : cloudEncryptionKeys.values()) {
            if (key.getId() == keyId) {
                decryptionKey = key;
                break;
            }
        }

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