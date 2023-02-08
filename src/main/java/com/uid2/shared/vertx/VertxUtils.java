package com.uid2.shared.vertx;

import com.uid2.shared.Const;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VertxUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxUtils.class);

    public static JsonObject getJsonConfig(Vertx vertx) throws ExecutionException, InterruptedException {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        VertxUtils.createConfigRetriever(vertx).getConfig(ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
            }
            else {
                future.complete(ar.result());
            }
        });
        return future.get();
    }

    // See also https://www.codota.com/web/assistant/code/rs/5c6569621095a500014beed7#L62
    // Create config retriever that is slightly different than the default implementation:
    // instead of 1 default config file, it provides 2 config files, one default config that
    // provides default values but can be override by vertx, env or sys store.
    // another override config that supersedes everything else.
    //
    // Order of config evaluation:
    // 1. A conf/default-config.json file.
    // 2. The Vert.x verticle config(), config() object can be customized by providing DeploymentOptions with vertx.deployVerticle
    // 3. The system properties
    // 4. The environment variables
    // 5. A conf/config.json file. This path can be overridden using the vertx-config-path system property or VERTX_CONFIG_PATH environment variable.
    // see https://vertx.io/blog/vert-x-application-configuration/
    //
    public static ConfigRetriever createConfigRetriever(Vertx vertx) {
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions();

        {
            String defaultConfigPath = getDefaultConfigPath(vertx);
            if (defaultConfigPath != null && !defaultConfigPath.trim().isEmpty()) {
                String format = extractFormatFromFileExtension(defaultConfigPath);
                LOGGER.info("Default config file path: " + defaultConfigPath + ", format:" + format);
                ConfigStoreOptions defaultStore = new ConfigStoreOptions()
                    .setType("file").setFormat(format)
                    .setConfig(new JsonObject().put("path", defaultConfigPath));
                retrieverOptions.addStore(defaultStore);
            }
        }

        ConfigStoreOptions vertxConfig = new ConfigStoreOptions().setType("json")
            .setConfig(vertx.getOrCreateContext().config());
        ConfigStoreOptions sysConfig = new ConfigStoreOptions().setType("sys");
        ConfigStoreOptions envConfig = new ConfigStoreOptions().setType("env");
        retrieverOptions.addStore(vertxConfig).addStore(sysConfig).addStore(envConfig);

        {
            String overrideConfigPath = getOverrideConfigPath(vertx);
            if (overrideConfigPath != null && !overrideConfigPath.trim().isEmpty()) {
                String format = extractFormatFromFileExtension(overrideConfigPath);
                LOGGER.info("Override config file path: " + overrideConfigPath + ", format:" + format);
                ConfigStoreOptions overrideStore = new ConfigStoreOptions()
                    .setType("file").setFormat(format)
                    .setConfig(new JsonObject().put("path", overrideConfigPath));
                retrieverOptions.addStore(overrideStore);
            }
        }

        return ConfigRetriever.create(vertx, retrieverOptions);
    }

    static String extractFormatFromFileExtension(String path) {
        int index = path.lastIndexOf(".");
        if (index == -1) {
            // Default format
            return "json";
        } else {
            String ext = path.substring(index + 1);
            if (ext.trim().isEmpty()) {
                return "json";
            }

            if ("yml".equalsIgnoreCase(ext)) {
                ext = "yaml";
            }
            return ext.toLowerCase();
        }
    }

    private static String getDefaultConfigPath(Vertx vertx) {
        String value = System.getenv("VERTX_DEFAULT_CONFIG_PATH");
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(Const.Config.VERTX_DEFAULT_CONFIG_PATH_PROP);
        }
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        boolean exists = vertx.fileSystem().existsBlocking(Const.Config.DEFAULT_CONFIG_PATH);
        if (exists) {
            return Const.Config.DEFAULT_CONFIG_PATH;
        }
        return null;
    }

    private static String getOverrideConfigPath(Vertx vertx) {
        // honor vertx-config-path, as it is essentially the override config that supersedes everything else
        String value = System.getenv("VERTX_CONFIG_PATH");
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(Const.Config.VERTX_CONFIG_PATH_PROP);
        }
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        boolean exists = vertx.fileSystem().existsBlocking(Const.Config.OVERRIDE_CONFIG_PATH);
        if (exists) {
            return Const.Config.OVERRIDE_CONFIG_PATH;
        }
        return null;
    }
}
