package com.uid2.shared.vertx;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class CoreConfigStoreFactory implements ConfigStoreFactory {
    @Override
    public String name() {
        return "core";
    }

    @Override
    public ConfigStore create(Vertx vertx, JsonObject configuration) {
        return new CoreConfigStore(vertx, configuration);
    }
}
