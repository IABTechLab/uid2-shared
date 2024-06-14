package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.Site;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ISiteStore;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.SiteParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Map;


public class RotatingSiteStore implements ISiteStore, StoreReader<Collection<Site>> {
    private final ScopedStoreReader<Map<Integer, Site>> reader;

    public RotatingSiteStore(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new SiteParser(), "sites");
    }

    @Override
    public CloudPath getMetadataPath() { return reader.getMetadataPath(); }

    @Override
    public Collection<Site> getAllSites() {
        return getAll();
    }

    @Override
    public Collection<Site> getAll() {
        return reader.getSnapshot().values();
    }

    @Override
    public Site getSite(int siteId) {
        final var snapshot = reader.getSnapshot();
        return snapshot != null ? snapshot.get(siteId) : null;
    }

    public JsonObject getMetadata() throws Exception {
        return reader.getMetadata();
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        return reader.loadContent(metadata, "sites");
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }
}
