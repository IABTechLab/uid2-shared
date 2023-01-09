package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;

public interface StoreScope {
    Integer getId();
    CloudPath getMetadataPath();
    CloudPath resolve(CloudPath cloudPath);

}

