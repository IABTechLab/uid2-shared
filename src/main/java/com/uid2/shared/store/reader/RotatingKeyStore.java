package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.EncryptedScopedStoreReader;
import com.uid2.shared.store.IKeyStore;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.KeyParser;
import com.uid2.shared.store.scope.EncryptedScope;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Collection;

/*
  1. metadata.json format

    {
      "version" : 1,
      "generated" : <unix_epoch_seconds>,
      "system_key_id": 3
      "keys" : {
        "location": "s3_path"
      }
    }

  2. keys.json format. SiteEncryptionKey has site_id, otherwise it's EncryptionKey

  [
    {
      "id": 3
      "created" : 1609459200,
      "expires" : 1893456000,
      "key_secret" : "<secret>"
    },
    {
      "id": 2
      "created" : 1609459200,
      "expires" : 1893456000,
      "key_secret" : "<secret>",
      "site_id": 2
    }
  ]

 */
public class RotatingKeyStore implements IKeyStore, StoreReader<Collection<EncryptionKey>> {
    private final ScopedStoreReader<IKeyStoreSnapshot> reader;

    public RotatingKeyStore(DownloadCloudStorage fileStreamProvider, StoreScope scope, RotatingS3KeyProvider s3KeyProvider) {
        this.reader = createReader(fileStreamProvider, scope, s3KeyProvider);
    }

    private ScopedStoreReader<IKeyStoreSnapshot> createReader(DownloadCloudStorage fileStreamProvider, StoreScope scope, RotatingS3KeyProvider s3KeyProvider) {
        if (scope instanceof EncryptedScope) {
            EncryptedScope encryptedScope = (EncryptedScope) scope;
            return new EncryptedScopedStoreReader<>(
                    fileStreamProvider,
                    encryptedScope,
                    new KeyParser(),
                    "keys",
                    encryptedScope.getId(),
                    s3KeyProvider
            );
        } else {
            return new ScopedStoreReader<>(
                    fileStreamProvider,
                    scope,
                    new KeyParser(),
                    "keys"
            );
        }
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public IKeyStoreSnapshot getSnapshot(Instant asOf) {
        return reader.getSnapshot();
    }

    @Override
    public IKeyStoreSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        return reader.getMetadata();
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        return reader.loadContent(metadata, "keys");
    }

    @Override
    public Collection<EncryptionKey> getAll() {
        return reader.getSnapshot().getActiveKeySet();
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }
}
