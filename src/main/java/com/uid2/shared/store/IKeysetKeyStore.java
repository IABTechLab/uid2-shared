package com.uid2.shared.store;

import java.time.Instant;

public interface IKeysetKeyStore {
    public KeysetKeyStoreSnapshot getSnapshot(Instant asOf);
    public KeysetKeyStoreSnapshot getSnapshot();
}
