
package com.uid2.shared.store.scope;


import com.uid2.shared.store.CloudPath;

public class EncryptedScope implements StoreScope {
    public EncryptedScope(CloudPath rootMetadataPath, Integer siteId){
        this.rootMetadataPath = rootMetadataPath;
        this.siteId = siteId;
    }
    private final Integer siteId;
    private final CloudPath rootMetadataPath;

    @Override
    public CloudPath getMetadataPath() {
        // Default to private for backward compatibility
        return getMetadataPath(false);
    }

    public CloudPath getMetadataPath(boolean isPublic) {
        return resolve(rootMetadataPath.getFileName(), isPublic);
    }

    /*
    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = rootMetadataPath.getParent();
        return directory.resolve("encryption").resolve("site").resolve(getId().toString()).resolve(cloudPath);
    }
     */
    public CloudPath resolve(CloudPath cloudPath) {
        // Default to private for backward compatibility
        return resolve(cloudPath, false);
    }

    public CloudPath resolve(CloudPath cloudPath, boolean isPublic) {
        CloudPath directory = rootMetadataPath.getParent();
        String siteType = isPublic ? "public" : "private";
        return directory.resolve("encrypted").resolve(siteId + "_" + siteType).resolve(cloudPath);
    }

    @Override
    public Integer getId() {
        return siteId;
    }
}
