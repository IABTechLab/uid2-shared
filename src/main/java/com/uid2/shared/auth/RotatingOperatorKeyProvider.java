package com.uid2.shared.auth;

import com.uid2.shared.Utils;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.IMetadataVersionedStore;
import com.uid2.shared.store.IOperatorKeyProvider;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RotatingOperatorKeyProvider implements IOperatorKeyProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingOperatorKeyProvider.class);

    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private final AtomicReference<Map<String, OperatorKey>> latestSnapshot = new AtomicReference<Map<String, OperatorKey>>(null);

    public RotatingOperatorKeyProvider(ICloudStorage metadataStreamProvider, ICloudStorage contentStreamProvider, String metadataPath) {
        this.metadataStreamProvider = metadataStreamProvider;
        this.contentStreamProvider = contentStreamProvider;
        this.metadataPath = metadataPath;
    }

    public String getMetadataPath() {
        return metadataPath;
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        InputStream s = this.metadataStreamProvider.download(this.metadataPath);
        return Utils.toJsonObject(s);
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonObject operatorsMetadata = metadata.getJsonObject("operators");
        final String contentPath = operatorsMetadata.getString("location");
        final InputStream contentStream = this.contentStreamProvider.download(contentPath);
        return loadOperators(contentStream);
    }

    private long loadOperators(InputStream contentStream) throws Exception {
        JsonArray operators = Utils.toJsonArray(contentStream);
        Map<String, OperatorKey> keyMap = new HashMap<>();
        for (int i=0; i<operators.size(); ++i){
            JsonObject opSpec = operators.getJsonObject(i);
            OperatorKey opKey = OperatorKey.valueOf(opSpec);
            keyMap.put(opKey.getKey(), opKey);
        }
        this.latestSnapshot.set(keyMap);
        LOGGER.info("Loaded " + keyMap.size() + " operator profiles");
        return keyMap.size();
    }

    @Override
    public OperatorKey getOperatorKey(String token) {
        return this.latestSnapshot.get().get(token);
    }

    @Override
    public Collection<OperatorKey> getAll() {
        return this.latestSnapshot.get().values();
    }

    @Override
    public IAuthorizable get(String key) {
        return getOperatorKey(key);
    }
}
