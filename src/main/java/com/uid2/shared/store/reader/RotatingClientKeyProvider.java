package com.uid2.shared.store.reader;

import com.uid2.shared.auth.AuthorizableStore;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.IClientKeyProvider;
import com.uid2.shared.store.parser.ClientParser;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Map;

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
public class RotatingClientKeyProvider implements IClientKeyProvider, StoreReader<Collection<ClientKey>> {
    private final ScopedStoreReader<Map<String, ClientKey>> reader;
    private final AuthorizableStore<ClientKey> authorizableStore;

    public RotatingClientKeyProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ClientParser(), "auth keys");
        this.authorizableStore = new AuthorizableStore<>();
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        return reader.getMetadata();
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        long version = reader.loadContent(metadata, "client_keys");
        this.authorizableStore.refresh(this.getAll());
        return version;
    }

    @Override
    public ClientKey getClientKey(String token) {
        return reader.getSnapshot().get(token);
    }

    public ClientKey getClientKeyFromHash(String hash) {
        return this.authorizableStore.getFromKeyHash(hash);
    }

    @Override
    public Collection<ClientKey> getAll() {
        return reader.getSnapshot().values();
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public IAuthorizable get(String key) {
        return getClientKey(key);
    }
}
