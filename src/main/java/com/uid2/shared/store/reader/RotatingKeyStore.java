package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.IKeyStore;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.KeyParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

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
public class RotatingKeyStore implements IKeyStore, IMetadataVersionedStore {
    private final ScopedStoreReader<IKeyStoreSnapshot> reader;

    public RotatingKeyStore(ICloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new KeyParser(), "keys");
    }

    public CloudPath getMetadataPath() { return reader.getMetadataPath(); }

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

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

}
