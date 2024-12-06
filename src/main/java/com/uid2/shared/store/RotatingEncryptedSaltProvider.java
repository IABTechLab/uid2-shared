package com.uid2.shared.store;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;

import com.uid2.shared.store.reader.StoreReader;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

public class RotatingEncryptedSaltProvider extends RotatingSaltProvider implements StoreReader<Collection<RotatingSaltProvider.SaltSnapshot>> {
    private final RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider;

    public RotatingEncryptedSaltProvider(DownloadCloudStorage fileStreamProvider, String metadataPath, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) {
        super(fileStreamProvider, metadataPath);
        this.cloudEncryptionKeyProvider = cloudEncryptionKeyProvider;
    }

    @Override
    protected String readInputStream(InputStream inputStream) throws IOException {
        String encryptedContent = super.readInputStream(inputStream);

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

    @Override
    public Collection<SaltSnapshot> getAll() {
        return super.getSnapshots();
    }
}
