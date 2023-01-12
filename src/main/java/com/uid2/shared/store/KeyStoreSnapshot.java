package com.uid2.shared.store;

import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.model.EncryptionKey;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class KeyStoreSnapshot implements IKeyStore.IKeyStoreSnapshot {
    private final HashMap<Integer, EncryptionKey> keyMap;
    private final HashMap<Integer, List<EncryptionKey>> siteKeyMap;
    private final List<EncryptionKey> activeKeySet;

    public KeyStoreSnapshot(HashMap<Integer, EncryptionKey> keyMap, HashMap<Integer, List<EncryptionKey>> siteKeyMap) {
        this.keyMap = keyMap;
        this.siteKeyMap = siteKeyMap;
        this.activeKeySet = new ArrayList<>(keyMap.values());

        for(Map.Entry<Integer, List<EncryptionKey>> entry : siteKeyMap.entrySet()) {
            entry.getValue().sort(Comparator.comparing(EncryptionKey::getActivates));
        }
    }

    @Override
    public EncryptionKey getMasterKey(Instant now) {
        return this.getActiveSiteKey(Const.Data.MasterKeySiteId, now);
    }

    @Override
    public EncryptionKey getRefreshKey(Instant now) {
        return this.getActiveSiteKey(Const.Data.RefreshKeySiteId, now);
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