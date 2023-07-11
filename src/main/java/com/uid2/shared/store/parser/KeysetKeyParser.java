package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.IKeysetKeyStore;
import com.uid2.shared.store.KeysetKeyStoreSnapshot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class KeysetKeyParser implements Parser<IKeysetKeyStore.IkeysetKeyStoreSnapshot> {
    @Override
    public ParsingResult<IKeysetKeyStore.IkeysetKeyStoreSnapshot> deserialize(InputStream inputStream) throws IOException {
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, KeysetKey> keyMap = new HashMap<>();
        final HashMap<Integer, List<KeysetKey>> keysetMap = new HashMap<>();
        for (int i = 0; i < keysSpec.size(); i++) {
            JsonObject keySpec = keysSpec.getJsonObject(i);
            int keysetId = keySpec.getInteger("keyset_id");
            Instant created = Instant.ofEpochSecond(keySpec.getLong("created"));
            Instant activates = Instant.ofEpochSecond(keySpec.getLong("activates"));
            Instant expires = Instant.ofEpochSecond(keySpec.getLong("expires"));
            KeysetKey keysetKey = new KeysetKey(
                    keySpec.getInteger("id"),
                    Base64.getDecoder().decode(keySpec.getString("secret")),
                    created, activates, expires, keysetId
                    );
            keyMap.put(keysetKey.getId(), keysetKey);
            keysetMap.computeIfAbsent(keysetId, k -> new ArrayList<>()).add(keysetKey);
        }
        KeysetKeyStoreSnapshot snapshot = new KeysetKeyStoreSnapshot(keyMap, keysetMap);
        return new ParsingResult<>(snapshot, keysSpec.size());
    }
}
