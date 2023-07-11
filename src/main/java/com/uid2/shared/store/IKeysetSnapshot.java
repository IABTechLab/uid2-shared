package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.Keyset;
import com.uid2.shared.model.KeysetKey;

public interface IKeysetSnapshot {
    boolean canClientAccessKey(ClientKey clientKey, KeysetKey key);
    Keyset getKeyset(int keysetId);
}
