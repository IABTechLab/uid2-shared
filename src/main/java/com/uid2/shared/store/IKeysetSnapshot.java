package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.Keyset;
import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;

import java.util.Map;

public interface IKeysetSnapshot {
    boolean canClientAccessKey(ClientKey clientKey, KeysetKey key);
    boolean canClientAccessKey(ClientKey clientKey, KeysetKey key, MissingAclMode accessMethod);
    Keyset getKeyset(int keysetId);
    Map<Integer, Keyset> getAllKeysets();
}