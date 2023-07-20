package com.uid2.shared.auth;

import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;

import java.util.Map;

public class KeysetSnapshot {
    private final Map<Integer, Keyset> keysets;

    public KeysetSnapshot(Map<Integer, Keyset> keysets) { this.keysets = keysets; }

    public boolean canClientAccessKey(ClientKey clientKey, KeysetKey key, MissingAclMode accessMethod) {
        Keyset keyset = keysets.get(key.getKeysetId());

        if (keyset == null || !keyset.isEnabled() || clientKey == null) return false;

        if (keyset.getSiteId() == clientKey.getSiteId()) return true;

        if (accessMethod == MissingAclMode.ALLOW_ALL
            && keyset.getAllowedSites() == null) return true;

        return keyset.canBeAccessedBySite(clientKey.getSiteId());
    }

    public Map<Integer, Keyset> getAllKeysets() { return this.keysets; }

    public Keyset getKeyset(int keysetId) { return this.keysets.get(keysetId); }
}
