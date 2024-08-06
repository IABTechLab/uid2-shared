package com.uid2.shared.store.reader;

import com.uid2.shared.auth.AuthorizableStore;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.EncryptedScopedStoreReader;
import com.uid2.shared.store.IClientKeyProvider;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.parser.ClientParser;
import com.uid2.shared.store.scope.EncryptedScope;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Comparator;

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
    private final ScopedStoreReader<Collection<ClientKey>> reader;
    private final AuthorizableStore<ClientKey> authorizableStore;

    public RotatingClientKeyProvider(DownloadCloudStorage fileStreamProvider, StoreScope scope) {
        this.reader = new ScopedStoreReader<>(fileStreamProvider, scope, new ClientParser(), "auth keys");
        this.authorizableStore = new AuthorizableStore<>(ClientKey.class);
    }

    public RotatingClientKeyProvider(DownloadCloudStorage fileStreamProvider, EncryptedScope scope, RotatingS3KeyProvider s3KeyProvider) {
        this.reader = new EncryptedScopedStoreReader<>(fileStreamProvider, scope, new ClientParser(), "auth keys", s3KeyProvider);
        this.authorizableStore = new AuthorizableStore<>(ClientKey.class);
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
        authorizableStore.refresh(getAll());
        return version;
    }

    @Override
    public ClientKey getClientKey(String key) {
        return authorizableStore.getAuthorizableByKey(key);
    }

    @Override
    public ClientKey getClientKeyFromHash(String hash) {
        return authorizableStore.getAuthorizableByHash(hash);
    }

    @Override
    public Collection<ClientKey> getAll() {
        return reader.getSnapshot();
    }

    @Override
    public void loadContent() throws Exception {
        loadContent(getMetadata());
    }

    @Override
    public CloudPath getMetadataPath() {
        return reader.getMetadataPath();
    }

    @Override
    public IAuthorizable get(String key) {
        return getClientKey(key);
    }

    @Override
    public ClientKey getOldestClientKey(int siteId) {
        return this.reader.getSnapshot().stream()
                .filter(k -> k.getSiteId() == siteId) // filter by site id
                .sorted(Comparator.comparing(ClientKey::getCreated)) // sort by key creation timestamp ascending
                .findFirst() // return the oldest key
                .orElse(null);
    }
}
