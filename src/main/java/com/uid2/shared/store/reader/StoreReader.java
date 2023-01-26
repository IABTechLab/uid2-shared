package com.uid2.shared.store.reader;

import com.uid2.shared.store.CloudPath;
import io.vertx.core.json.JsonObject;

public interface StoreReader<T> extends IMetadataVersionedStore {
    T getAll();
    void loadContent() throws Exception;
    JsonObject getMetadata() throws Exception;

    CloudPath getMetadataPath();
}
