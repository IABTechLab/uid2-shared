package com.uid2.shared.store.scope;


import com.uid2.shared.store.CloudPath;

public class SiteScope implements StoreScope {
    public SiteScope(CloudPath rootMetadataPath, Integer siteId){
        this.rootMetadataPath = rootMetadataPath;
        this.siteId = siteId;
    }
    private final Integer siteId;
    private final CloudPath rootMetadataPath;


    @Override
    public CloudPath getMetadataPath() {
        return resolve(rootMetadataPath.getFileName());
    }

    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = rootMetadataPath.getParent();
        return directory.resolve("site").resolve(siteId.toString()).resolve(cloudPath);
    }

    @Override
    public Integer getId() {
        return siteId;
    }
}
