package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;

public class EncryptedScope extends SiteScope {

    public EncryptedScope(CloudPath rootMetadataPath, Integer siteId) {
        super(rootMetadataPath, siteId);
    }

    @Override
    public CloudPath getMetadataPath() {
        return resolve(super.getMetadataPath().getFileName());
    }

    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = super.getMetadataPath().getParent();
        return directory.resolve("encryption").resolve("site").resolve(getId().toString()).resolve(cloudPath);
    }

}