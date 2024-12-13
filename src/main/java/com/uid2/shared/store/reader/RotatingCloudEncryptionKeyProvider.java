package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.CloudEncryptionKeyParser;
import com.uid2.shared.store.scope.StoreScope;
import com.uid2.shared.model.CloudEncryptionKey;
import io.vertx.core.json.JsonObject;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class RotatingCloudEncryptionKeyProvider implements StoreReader<Map<Integer, CloudEncryptionKey>> {
    protected ScopedStoreReader<Map<Integer, CloudEncryptionKey>> reader;

    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingCloudEncryptionKeyProvider.class);
    public Map<Integer, List<CloudEncryptionKey>> siteToKeysMap = new HashMap<>();

    public RotatingCloudEncryptionKeyProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new CloudEncryptionKeyParser(), "cloud_encryption_keys");
    }


    public RotatingCloudEncryptionKeyProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope, ScopedStoreReader<Map<Integer, CloudEncryptionKey>> reader) {
        this.reader = reader;
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
        long result = reader.loadContent(metadata, "cloud_encryption_keys");
        updateSiteToKeysMapping();
        return result;
    }

    @Override
    public Map<Integer, CloudEncryptionKey> getAll() {
        Map<Integer, CloudEncryptionKey> keys = reader.getSnapshot();
        return keys != null ? keys : new HashMap<>();
    }

    public CloudEncryptionKey getKey(int id) {
        Map<Integer, CloudEncryptionKey> snapshot = reader.getSnapshot();
        if(snapshot == null) {
            return null;
        }

        return snapshot.get(id);
    }

    public void updateSiteToKeysMapping() {
        Map<Integer, CloudEncryptionKey> allKeys = getAll();
        siteToKeysMap.clear();
        allKeys.values().forEach(key ->
                this.siteToKeysMap
                        .computeIfAbsent(key.getSiteId(), k -> new ArrayList<>())
                        .add(key)
        );
        LOGGER.info("Updated site-to-keys mapping for {} sites", siteToKeysMap.size());
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public Set<Integer> getAllSiteIds() {
        return new HashSet<>(siteToKeysMap.keySet());
    }

    public int getTotalSites() {
        return siteToKeysMap.size();
    }

    public List<CloudEncryptionKey> getKeys(int siteId) {
        //for s3 encryption keys retrieval
        return siteToKeysMap.getOrDefault(siteId, new ArrayList<>());
    }

    public Collection<CloudEncryptionKey> getKeysForSite(Integer siteId) {
        Map<Integer, CloudEncryptionKey> allKeys = getAll();
        return allKeys.values().stream()
                .filter(key -> key.getSiteId() == (siteId))
                .collect(Collectors.toList());
    }

    public CloudEncryptionKey getEncryptionKeyForSite(Integer siteId) {
        //get the youngest activated key
        Collection<CloudEncryptionKey> keys = getKeysForSite(siteId);
        long now = Instant.now().getEpochSecond();
        if (keys.isEmpty()) {
            throw new IllegalStateException("No S3 keys available for encryption for site ID: " + siteId);
        }
        return keys.stream()
                .filter(key -> key.getActivates() <= now)
                .max(Comparator.comparingLong(CloudEncryptionKey::getCreated))
                .orElseThrow(() -> new IllegalStateException("No active keys found for site ID: " + siteId));
    }
}
