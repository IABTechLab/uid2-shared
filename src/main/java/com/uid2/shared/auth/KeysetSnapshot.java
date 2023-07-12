package com.uid2.shared.auth;

import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;
import com.uid2.shared.store.IKeysetSnapshot;

import java.util.Map;

public class KeysetSnapshot implements IKeysetSnapshot {
    private final Map<Integer, Keyset> keysets;

    public KeysetSnapshot(Map<Integer, Keyset> keysets) { this.keysets = keysets; }

    @Override
    public boolean canClientAccessKey(ClientKey clientKey, KeysetKey key) {
        Keyset keyset = keysets.get(key.getKeysetId());

        if(!keyset.isEnabled()) return false;

        if(keyset.getSiteId() == clientKey.getSiteId()) return true;

        return keyset.canBeAccessedBySite(clientKey.getSiteId());
    }
    @Override
    public boolean canClientAccessKey(ClientKey clientKey, KeysetKey key, MissingAclMode accessMethod) {
        Keyset keyset = keysets.get(key.getKeysetId());

        if (keyset.isEnabled()
                && accessMethod == MissingAclMode.ALLOW_ALL
                && keyset.getAllowedSites() == null) return true;

        return this.canClientAccessKey(clientKey, key);
    }

    public Map<Integer, Keyset> getAllKeysets() { return this.keysets; }

    @Override
    public Keyset getKeyset(int keysetId) { return this.keysets.get(keysetId); }
}
