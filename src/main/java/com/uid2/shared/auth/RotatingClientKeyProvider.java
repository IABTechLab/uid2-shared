package com.uid2.shared.auth;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.IClientKeyProvider;
import com.uid2.shared.store.IMetadataVersionedStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/*
  1. metadata.json format

    {
      "version" : <long>,
      "generated" : <unix_epoch_seconds>,
      "client_keys" : {
        "location": "s3_path"
      }
    }

  2. client keys file format
      [
        {
           key = "aksjdkajsdkajsdja",
           name = "ClientName",
           contact = "ClientEmail",
           created = "timestamp",
           site_id = N,
           roles = [],
        },
        ...
      ]
*/
public class RotatingClientKeyProvider implements IClientKeyProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingClientKeyProvider.class);

    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private final AtomicReference<Map<String, ClientKey>> latestSnapshot = new AtomicReference<Map<String, ClientKey>>(new HashMap<>());


    public RotatingClientKeyProvider(ICloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = fileStreamProvider;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
        this.metadataPath = metadataPath;
    }

    public RotatingClientKeyProvider(UidCoreClient uidCoreClient, String metadataPath) {
        this.metadataStreamProvider = uidCoreClient;
        this.contentStreamProvider = uidCoreClient.getContentStorage();
        this.metadataPath = metadataPath;
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        InputStream s = this.metadataStreamProvider.download(this.metadataPath);
        return Utils.toJsonObject(s);
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonObject clientKeysMetadata = metadata.getJsonObject("client_keys");
        final String path = clientKeysMetadata.getString("location");
        final InputStream inputStream = this.contentStreamProvider.download(path);
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        Map<String, ClientKey> keyMap = new HashMap<>();
        for (int i = 0; i < keysSpec.size(); ++i) {
            JsonObject keySpec = keysSpec.getJsonObject(i);
            ClientKey clientKey = ClientKey.valueOf(keySpec);
            keyMap.put(clientKey.getKey(), clientKey);
        }
        this.latestSnapshot.set(keyMap);
        LOGGER.info("Loaded " + keysSpec.size() + " auth keys");
        return keysSpec.size();
    }

    @Override
    public ClientKey getClientKey(String token) {
        return latestSnapshot.get().get(token);
    }

    @Override
    public Collection<ClientKey> getAll() {
        return latestSnapshot.get().values();
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public String getMetadataPath() {
        return metadataPath;
    }

    @Override
    public IAuthorizable get(String key) {
        return getClientKey(key);
    }
}
