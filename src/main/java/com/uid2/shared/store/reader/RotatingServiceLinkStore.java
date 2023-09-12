package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.ServiceLink;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.IServiceLinkStore;
import com.uid2.shared.store.parser.ServiceLinkParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Map;

public class RotatingServiceLinkStore implements IServiceLinkStore, StoreReader<Collection<ServiceLink>> {
    private final ScopedStoreReader<Map<String, ServiceLink>> reader;

    public RotatingServiceLinkStore(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ServiceLinkParser(), "service_links");
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
        return reader.loadContent(metadata, "service_links");
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public Collection<ServiceLink> getAllServiceLinks() {
        return getAll();
    }

    @Override
    public ServiceLink getServiceLink(int serviceId, String linkId) {
        return reader.getSnapshot().get(serviceId + "_" + linkId);
    }

    @Override
    public Collection<ServiceLink> getAll() {
        return reader.getSnapshot().values();
    }

    @Override
    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }
}
