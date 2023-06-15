package com.uid2.shared.store;

import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.Utils;

import java.time.Instant;
import java.util.*;

public class KeysetKeyStoreSnapshot implements IKeysetKeyStore.IkeysetKeyStoreSnapshot {
    private final HashMap<Integer, KeysetKey> keyMap;
    private final HashMap<Integer, List<KeysetKey>> keysetMap;
    private final List<KeysetKey> activeKeys;

    public KeysetKeyStoreSnapshot(HashMap<Integer, KeysetKey> keyMap, HashMap<Integer, List<KeysetKey>> keysetMap) {
        this.keyMap = keyMap;
        this.keysetMap = keysetMap;
        this.activeKeys = new ArrayList<>(keyMap.values());

        for(Map.Entry<Integer, List<KeysetKey>> entry : keysetMap.entrySet()) {
            entry.getValue().sort(Comparator.comparing(KeysetKey::getActivates));
        }
    }

    @Override
    public List<KeysetKey> getActiveKeysetKeys() {
        return this.activeKeys;
    }

    @Override
    public KeysetKey getActiveKey(int keysetId, Instant now) {
        List<KeysetKey> keysetKeys = keysetMap.get(keysetId);
        if(keysetKeys == null || keysetKeys.isEmpty()) return null;
        int it = Utils.upperBound(keysetKeys, now, (ts, k) -> ts.isBefore(k.getActivates()));
        while(it > 0) {
            KeysetKey key = keysetKeys.get(it-1);
            if(!key.isExpired(now)) {
                return key;
            }
            --it;
        }
        return null;
    }

    @Override
    public KeysetKey getKey(int keyId) {
        try {
            return this.keyMap.get(keyId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Key ID " + keyId + " not supported");
        }
    }
}
