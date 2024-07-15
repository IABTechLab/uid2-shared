package com.uid2.shared.store.reader;

import com.uid2.shared.auth.KeysetSnapshot;
import com.uid2.shared.auth.RotatingOperatorKeyProvider;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.S3KeyParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.uid2.shared.model.S3Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RotatingS3KeyProvider implements StoreReader<Map<Integer, S3Key>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingOperatorKeyProvider.class);

    ScopedStoreReader<Map<Integer, S3Key>> reader;

    public RotatingS3KeyProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new S3KeyParser(), "s3encryption_keys");
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        return reader.getMetadata();
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }


    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        long loadedKeysCount = reader.loadContent(metadata, "s3encryption_keys");
        LOGGER.info("Loaded {} S3 encryption keys", loadedKeysCount);
        return loadedKeysCount;    }

    @Override
    public Map<Integer, S3Key> getAll() {
        Map<Integer, S3Key> keys = reader.getSnapshot();
        return keys != null ? keys : new HashMap<>();
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public Collection<S3Key> getKeysForSite(Integer siteId) {
        Map<Integer, S3Key> allKeys = getAll();
        return allKeys.values().stream()
                .filter(key -> key.getSiteId()==(siteId))
                .collect(Collectors.toList());
    }

    public S3Key getEncryptionKeyForSite(Integer siteId) {
        Collection<S3Key> keys = getKeysForSite(siteId);
        if (keys.isEmpty()) {
            throw new IllegalStateException("No S3 keys available for encryption for site ID: " + siteId);
        } else {
            Map<Integer, S3Key> allKeys = getAll();
            S3Key largestKey = null;
            for (S3Key key : allKeys.values()) {
                if (key.getSiteId() == siteId) {
                    if (largestKey == null || key.getId() > largestKey.getId()) {
                        largestKey = key;
                    }
                }
            }
            return largestKey;
        }
    }
}
