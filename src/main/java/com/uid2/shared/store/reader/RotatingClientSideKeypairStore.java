package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.ClientSideKeypair;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.IClientSideKeypairStore;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.ClientSideKeypairParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Collection;

public class RotatingClientSideKeypairStore implements IClientSideKeypairStore, StoreReader<Collection<ClientSideKeypair>> {
    private final ScopedStoreReader<IClientSideKeypairStoreSnapshot> reader;

    public RotatingClientSideKeypairStore(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ClientSideKeypairParser(), "client_side_keypairs");
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        return reader.loadContent(metadata, "client_side_keypairs");
    }

    @Override
    public Collection<ClientSideKeypair> getAll() {
        return reader.getSnapshot().getAll();
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
    public CloudPath getMetadataPath(){
        return reader.getMetadataPath();
    }

    @Override
    public IClientSideKeypairStoreSnapshot getSnapshot(Instant asOf) {
        return reader.getSnapshot();
    }

    @Override
    public IClientSideKeypairStoreSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
    }

}
