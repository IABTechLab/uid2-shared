package com.uid2.shared.store;

import com.uid2.shared.model.EncryptionKey;

import java.time.Instant;
import java.util.List;

public interface IKeyStore {
    public IKeyStoreSnapshot getSnapshot(Instant asOf);

    public IKeyStoreSnapshot getSnapshot();

    public interface IKeyStoreSnapshot {
        public EncryptionKey getMasterKey(Instant now);

        public EncryptionKey getRefreshKey(Instant now);

        public List<EncryptionKey> getActiveKeySet();

        public EncryptionKey getActiveSiteKey(int siteId, Instant now);

        public EncryptionKey getKey(int keyId);
    }
}
