package com.uid2.shared.store;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.S3Key;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.EncryptedScope;
import com.uid2.shared.store.scope.StoreScope;
import com.uid2.shared.store.reader.RotatingS3KeyProvider;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import com.uid2.shared.encryption.AesGcm;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class EncryptedScopedStoreReader<T> extends ScopedStoreReader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedScopedStoreReader.class);

    private final int siteId;
    private final RotatingS3KeyProvider s3KeyProvider;

    public EncryptedScopedStoreReader(DownloadCloudStorage fileStreamProvider, EncryptedScope scope, Parser<T> parser, String dataTypeName, RotatingS3KeyProvider s3KeyProvider) {
        super(fileStreamProvider, scope, parser, dataTypeName);
        this.siteId = scope.getId();
        this.s3KeyProvider = s3KeyProvider;
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

        Map<Integer, S3Key> s3Keys = s3KeyProvider.getAll();
        S3Key decryptionKey = null;

        for (S3Key key : s3Keys.values()) {
            if (key.getSiteId() == siteId && key.getId() == keyId) {
                decryptionKey = key;
                break;
            }
        }

        if (decryptionKey == null) {
            throw new IllegalStateException("No matching S3 key found for decryption for site ID: " + siteId + " and key ID: " + keyId);
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