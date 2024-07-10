package com.uid2.shared.store.reader;

import com.uid2.shared.auth.AclSnapshot;
import com.uid2.shared.auth.EncryptionKeyAcl;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.EncryptedScopedStoreReader;
import com.uid2.shared.store.IKeyAclProvider;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.KeyAclParser;
import com.uid2.shared.store.scope.EncryptedScope;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Map;

public class RotatingKeyAclProvider implements IKeyAclProvider, StoreReader<Map<Integer, EncryptionKeyAcl>> {
    private final ScopedStoreReader<AclSnapshot> reader;

    public RotatingKeyAclProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new KeyAclParser(), "key acls");
    }

    public RotatingKeyAclProvider(DownloadCloudStorage fileStreamProvider, EncryptedScope scope, RotatingS3KeyProvider s3KeyProvider) {
        this.reader =  new EncryptedScopedStoreReader<>(fileStreamProvider, scope, new KeyAclParser(), "key acls", scope.getId(), s3KeyProvider);
    }

    @Override
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

    @Override
    public Map<Integer, EncryptionKeyAcl> getAll() {
        return reader.getSnapshot().getAllAcls();
    }

    @Override
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
