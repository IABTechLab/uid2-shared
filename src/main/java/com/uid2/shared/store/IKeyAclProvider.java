package com.uid2.shared.store;

import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.auth.ClientKey;

import java.time.Instant;

public interface IKeyAclProvider {
    IKeysAclSnapshot getSnapshot(Instant asOf);
    IKeysAclSnapshot getSnapshot();
}
