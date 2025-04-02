package com.uid2.shared.store;

import io.vertx.core.json.JsonObject;

public interface IConfigStore {
    JsonObject getConfig();
}
