package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.ACLMode.LegacyDEP;

public interface IKeysAclSnapshot {
    boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key);
    boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key, LegacyDEP accessMethod);
}

