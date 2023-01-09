package com.uid2.shared.store.reader;

import com.uid2.shared.auth.AclSnapshot;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.*;
import com.uid2.shared.store.parser.KeyAclParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

public class RotatingKeyAclProvider implements IKeyAclProvider, IMetadataVersionedStore {
    private final ScopedStoreReader<AclSnapshot> reader;

    public RotatingKeyAclProvider(ICloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new KeyAclParser(), "key acls");
    }

    public CloudPath getMetadataPath() { return reader.getMetadataPath(); }

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
        return reader.loadContent(metadata, "keys_acl");
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    @Override
    public AclSnapshot getSnapshot(Instant asOf) {
        return reader.getSnapshot();
    }

    @Override
    public AclSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
    }

}
