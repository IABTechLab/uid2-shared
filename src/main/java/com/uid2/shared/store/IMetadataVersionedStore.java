package com.uid2.shared.store;

import io.vertx.core.json.JsonObject;

public interface IMetadataVersionedStore {
    JsonObject getMetadata() throws Exception;

    long getVersion(JsonObject metadata);

    long loadContent(JsonObject metadata) throws Exception;
}
