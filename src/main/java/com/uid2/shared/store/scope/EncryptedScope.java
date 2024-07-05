
package com.uid2.shared.store.scope;


import com.uid2.shared.store.CloudPath;

public class EncryptedScope implements StoreScope {
    public EncryptedScope(CloudPath rootMetadataPath, Integer siteId) {
        this(rootMetadataPath, siteId, false);
    }

    public EncryptedScope(CloudPath rootMetadataPath, Integer siteId, boolean isPublic){
        this.rootMetadataPath = rootMetadataPath;
        this.siteId = siteId;
        this.isPublic=isPublic;
    }
    private final Integer siteId;
    private final CloudPath rootMetadataPath;
    private final boolean isPublic;


    @Override
    public CloudPath getMetadataPath() {
        // Default to private for backward compatibility
        return getMetadataPath(false);
    }

    public CloudPath getMetadataPath(boolean isPublic) {
        return resolve(rootMetadataPath.getFileName(), isPublic);
    }

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

    /* paths when we no longer need to distinguish private & public sites
    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = rootMetadataPath.getParent();
        return directory.resolve("encryption").resolve("site").resolve(getId().toString()).resolve(cloudPath);
    }
    */
}
