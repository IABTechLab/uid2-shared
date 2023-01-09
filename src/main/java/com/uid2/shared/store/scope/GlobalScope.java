package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;

public class GlobalScope implements StoreScope {
    private final CloudPath rootMetadataPath;

    public GlobalScope(CloudPath rootMetadataPath) {
        this.rootMetadataPath = rootMetadataPath;
    }

    @Override
    public CloudPath getMetadataPath() { return rootMetadataPath; }

    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        return rootMetadataPath.getParent().resolve(cloudPath);
    }

    @Override
    public Integer getId() {
        return null;
    }
}
