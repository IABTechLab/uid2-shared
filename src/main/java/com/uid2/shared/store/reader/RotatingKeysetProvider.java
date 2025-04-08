package com.uid2.shared.store.reader;

import com.uid2.shared.auth.Keyset;
import com.uid2.shared.auth.KeysetSnapshot;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.EncryptedScopedStoreReader;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.KeysetParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class RotatingKeysetProvider implements StoreReader<Map<Integer, Keyset>> {
    private final ScopedStoreReader<KeysetSnapshot> reader;

    public RotatingKeysetProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new KeysetParser(), "keysets");
    }

    public RotatingKeysetProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) {
        this.reader = new EncryptedScopedStoreReader<>(fileStreamProvider, scope, new KeysetParser(), "keysets", cloudEncryptionKeyProvider);
    }

    public KeysetSnapshot getSnapshot() {
        return reader.getSnapshot();
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        return reader.loadContent(metadata, "keysets");
    }

    @Override
    public Map<Integer, Keyset> getAll() {
        return reader.getSnapshot().getAllKeysets();
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        return reader.getMetadata();
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }
}
