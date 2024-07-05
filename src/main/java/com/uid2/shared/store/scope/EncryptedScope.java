
package com.uid2.shared.store.scope;


import com.uid2.shared.store.CloudPath;

public class EncryptedScope implements StoreScope {

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
        return resolve(rootMetadataPath.getFileName());
    }

    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = rootMetadataPath.getParent();
        String siteType = isPublic ? "public" : "private";
        return directory.resolve("encrypted").resolve(siteId + "_" + siteType).resolve(cloudPath);
    }

    @Override
    public Integer getId() {
        return siteId;
    }

    public boolean getPublic() {
        return isPublic;
    }

    /* paths when we no longer need to distinguish private & public sites
    @Override
    public CloudPath resolve(CloudPath cloudPath) {
        CloudPath directory = rootMetadataPath.getParent();
        return directory.resolve("encryption").resolve("site").resolve(getId().toString()).resolve(cloudPath);
    }
    */
}
