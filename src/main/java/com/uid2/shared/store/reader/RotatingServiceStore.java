package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.Service;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.IServiceStore;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.ServiceParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Map;

public class RotatingServiceStore implements IServiceStore, StoreReader<Collection<Service>> {

    private final ScopedStoreReader<Map<Integer, Service>> reader;

    public RotatingServiceStore(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ServiceParser(), "services");
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
    public Collection<Service> getAllServices() {
        return getAll();
    }

    @Override
    public Service getService(int serviceId) {
        return reader.getSnapshot().get(serviceId);
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        return reader.loadContent(metadata, "services");
    }

    @Override
    public Collection<Service> getAll() {
        return reader.getSnapshot().values();
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }
}
