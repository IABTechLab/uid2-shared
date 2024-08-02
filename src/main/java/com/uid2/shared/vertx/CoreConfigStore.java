package com.uid2.shared.vertx;

import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Utils;
import com.uid2.shared.attest.AttestationResponseHandler;
import com.uid2.shared.attest.NoAttestationProvider;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.CloudUtils;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.future.FailedFuture;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class CoreConfigStore implements ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreConfigStore.class);
    private final VertxInternal vertx;
    private final UidCoreClient uidCoreClient;

    public CoreConfigStore(Vertx vertx, JsonObject configuration) {
        this.vertx = (VertxInternal) vertx;
        String token = configuration.getString("core_api_token");
        String coreUrl = configuration.getString("core_attest_url");

        ApplicationVersion appVersion = null;
        try {
            appVersion = ApplicationVersion.load("uid2-operator", "uid2-shared", "uid2-attestation-api");
            AttestationResponseHandler handler = new AttestationResponseHandler(vertx, coreUrl, token, appVersion, new NoAttestationProvider(), null, CloudUtils.defaultProxy);
            this.uidCoreClient = new UidCoreClient(token, CloudUtils.defaultProxy, handler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Void> close() {
        return this.vertx.getOrCreateContext().succeededFuture();
    }

    @Override
    public Future<Buffer> get() {
        try {
            Future<JsonObject> future;
            InputStream stream = this.uidCoreClient.download("/config");
            String jsonString = Utils.readToEnd(stream);
            if (jsonString != null && !jsonString.isEmpty()) {
                return Future.succeededFuture(Buffer.buffer(jsonString));
            } else {
                return Future.failedFuture("Unable to get config from Core");
            }
        } catch (CloudStorageException e) {
            return Future.failedFuture(e);
        } catch (IOException e) {
            return Future.failedFuture(e);
        }
    }
}
