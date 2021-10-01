// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.store;

import com.uid2.shared.Const;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.ICloudStorage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/*
  1. metadata.json format

    {
      "version" : 1,
      "generated" : <unix_epoch_seconds>,
      "system_key_id": 3
      "keys" : {
        "location": "s3_path"
      }
    }

  2. keys.json format. SiteEncryptionKey has site_id, otherwise it's EncryptionKey

  [
    {
      "id": 3
      "created" : 1609459200,
      "expires" : 1893456000,
      "key_secret" : "<secret>"
    },
    {
      "id": 2
      "created" : 1609459200,
      "expires" : 1893456000,
      "key_secret" : "<secret>",
      "site_id": 2
    }
  ]

 */
public class RotatingKeyStore implements IKeyStore, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingKeyStore.class);

    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private AtomicReference<IKeyStoreSnapshot> latestSnapshot = new AtomicReference<IKeyStoreSnapshot>();

    public RotatingKeyStore(ICloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = fileStreamProvider;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
        this.metadataPath = metadataPath;
    }

    public String getMetadataPath() { return this.metadataPath; }

    @Override
    public IKeyStoreSnapshot getSnapshot(Instant asOf) {
        return this.latestSnapshot.get();
    }

    @Override
    public IKeyStoreSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
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
        final JsonObject clientKeysMetadata = metadata.getJsonObject("keys");
        final String path = clientKeysMetadata.getString("location");
        final InputStream inputStream = this.contentStreamProvider.download(path);
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, EncryptionKey> keyMap = new HashMap<>();
        final HashMap<Integer, List<EncryptionKey>> siteKeyMap = new HashMap<>();
        final Instant now = Instant.now();
        for (int i = 0; i < keysSpec.size(); ++i) {
            JsonObject keySpec = keysSpec.getJsonObject(i);
            int siteId = keySpec.getInteger("site_id");
            Instant created = Instant.ofEpochSecond(keySpec.getLong("created"));
            Instant activates = Instant.ofEpochSecond(keySpec.getLong("activates"));
            Instant expires = Instant.ofEpochSecond(keySpec.getLong("expires"));
            EncryptionKey key = new EncryptionKey(
                    keySpec.getInteger("id"),
                    Base64.getDecoder().decode(keySpec.getString("secret")),
                    created, activates, expires, siteId);
            keyMap.put(key.getId(), key);
            siteKeyMap.computeIfAbsent(siteId, k -> new ArrayList<EncryptionKey>()).add(key);
        }
        this.latestSnapshot.set(new KeyStoreSnapshot(keyMap, siteKeyMap));
        LOGGER.info("Loaded " + keysSpec.size() + " keys");
        return keysSpec.size();
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    public class KeyStoreSnapshot implements IKeyStoreSnapshot {
        private final HashMap<Integer, EncryptionKey> keyMap;
        private final HashMap<Integer, List<EncryptionKey>> siteKeyMap;
        private final List<EncryptionKey> activeKeySet;

        public KeyStoreSnapshot(HashMap<Integer, EncryptionKey> keyMap, HashMap<Integer, List<EncryptionKey>> siteKeyMap) {
            this.keyMap = keyMap;
            this.siteKeyMap = siteKeyMap;
            this.activeKeySet = keyMap.values().stream().collect(Collectors.toList());

            for(Map.Entry<Integer, List<EncryptionKey>> entry : siteKeyMap.entrySet()) {
                entry.getValue().sort(Comparator.comparing(EncryptionKey::getActivates));
            }
        }

        @Override
        public EncryptionKey getMasterKey(Instant now) {
            return this.getActiveSiteKey(Const.Data.MasterKeySiteId, now);
        }

        @Override
        public List<EncryptionKey> getActiveKeySet() {
            return this.activeKeySet;
        }

        @Override
        public EncryptionKey getActiveSiteKey(int siteId, Instant now) {
            List<EncryptionKey> siteKeys = siteKeyMap.get(siteId);
            if(siteKeys == null || siteKeys.isEmpty()) return null;
            int it = Utils.upperBound(siteKeys, now, (ts, k) -> ts.isBefore(k.getActivates()));
            while(it > 0) {
                EncryptionKey key = siteKeys.get(it-1);
                if(!key.isExpired(now)) {
                    return key;
                }
                --it;
            }
            return null;
        }

        @Override
        public EncryptionKey getKey(int keyId) {
            try {
                return this.keyMap.get(keyId);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Key ID " + keyId + " not supported");
            }
        }
    }
}
