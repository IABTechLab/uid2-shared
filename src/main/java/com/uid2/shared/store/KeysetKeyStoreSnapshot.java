package com.uid2.shared.store;

import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.Utils;

import java.time.Instant;
import java.util.*;

public class KeysetKeyStoreSnapshot {
    private final HashMap<Integer, KeysetKey> keyIdToKeysetKey;
    private final HashMap<Integer, List<KeysetKey>> keysetIdToKeysetKeyList;
    private final List<KeysetKey> allKeys;

    public KeysetKeyStoreSnapshot(HashMap<Integer, KeysetKey> keyIdToKeysetKey, HashMap<Integer, List<KeysetKey>> keysetIdToKeysetKeyList) {
        this.keyIdToKeysetKey = keyIdToKeysetKey;
        this.keysetIdToKeysetKeyList = keysetIdToKeysetKeyList;
        this.allKeys = new ArrayList<>(keyIdToKeysetKey.values());

        for(Map.Entry<Integer, List<KeysetKey>> entry : keysetIdToKeysetKeyList.entrySet()) {
            entry.getValue().sort(Comparator.comparing(KeysetKey::getActivates));
        }
    }

    public List<KeysetKey> getAllKeysetKeys() {
        return this.allKeys;
    }

    public KeysetKey getActiveKey(int keysetId, Instant now) {
        List<KeysetKey> keysetKeys = keysetIdToKeysetKeyList.get(keysetId);
        if(keysetKeys == null || keysetKeys.isEmpty()) return null;
        int keysetKeysIndex = Utils.upperBound(keysetKeys, now, (ts, k) -> ts.isBefore(k.getActivates()));
        while(keysetKeysIndex > 0) {
            KeysetKey key = keysetKeys.get(keysetKeysIndex-1);
            if(!key.isExpired(now)) {
                return key;
            }
            --keysetKeysIndex;
        }
        return null;
    }

    public KeysetKey getKey(int keyId) {
        try {
            return this.keyIdToKeysetKey.get(keyId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Key ID " + keyId + " not supported");
        }
    }
}
