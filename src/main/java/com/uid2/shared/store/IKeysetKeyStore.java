package com.uid2.shared.store;

import com.uid2.shared.model.KeysetKey;

import java.time.Instant;
import java.util.List;

public interface IKeysetKeyStore {
    public IkeysetKeyStoreSnapshot getSnapshot(Instant asOf);
    public IkeysetKeyStoreSnapshot getSnapshot();


    public interface IkeysetKeyStoreSnapshot {
        public List<KeysetKey> getAllKeysetKeys();
        public KeysetKey getActiveKey(int keysetId, Instant now);
        public KeysetKey getKey(int keyId);
    }
}
