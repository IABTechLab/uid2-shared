package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.S3KeyParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.uid2.shared.model.S3Key;

public class RotatingS3KeyProvider implements StoreReader<Map<Integer, S3Key>> {
    ScopedStoreReader<Map<Integer, S3Key>> reader;

    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingS3KeyProvider.class);
    public Map<Integer, List<S3Key>> siteToKeysMap = new HashMap<>();

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
        long result = reader.loadContent(metadata, "s3encryption_keys");
        updateSiteToKeysMapping();
        return result;
    }

    @Override
    public Map<Integer, S3Key> getAll() {
        Map<Integer, S3Key> keys = reader.getSnapshot();
        return keys != null ? keys : new HashMap<>();
    }

    public void updateSiteToKeysMapping() {
        Map<Integer, S3Key> allKeys = getAll();
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

    public Collection<S3Key> getKeysForSite(Integer siteId) {
        Map<Integer, S3Key> allKeys = getAll();
        return allKeys.values().stream()
                .filter(key -> key.getSiteId()==(siteId))
                .collect(Collectors.toList());
    }

    public List<S3Key> getKeys(int siteId) {
        return siteToKeysMap.getOrDefault(siteId, new ArrayList<>());
    }

     public S3Key getEncryptionKeyForSite(Integer siteId) {
        //get the youngest activated key
        Collection<S3Key> keys = getKeysForSite(siteId);
         long now = Instant.now().getEpochSecond();
         if (keys.isEmpty()) {
            throw new IllegalStateException("No S3 keys available for encryption for site ID: " + siteId);
        }
        return keys.stream()
                .filter(key -> key.getActivates() <= now)
                .max(Comparator.comparingLong(S3Key::getCreated))
                .orElseThrow(() -> new IllegalStateException("No active keys found for site ID: " + siteId));
    }
}
