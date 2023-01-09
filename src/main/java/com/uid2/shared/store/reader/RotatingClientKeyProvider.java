package com.uid2.shared.store.reader;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.cloud.ICloudStorage;
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
public class RotatingClientKeyProvider implements IClientKeyProvider, IMetadataVersionedStore {
    private final ScopedStoreReader<Map<String, ClientKey>> reader;
    public RotatingClientKeyProvider(ICloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ClientParser(), "auth keys");
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
        return reader.loadContent(metadata, "client_keys");
    }

    @Override
    public ClientKey getClientKey(String token) {
        return reader.getSnapshot().get(token);
    }

    @Override
    public Collection<ClientKey> getAll() {
        return reader.getSnapshot().values();
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public IAuthorizable get(String key) {
        return getClientKey(key);
    }
}
