package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.KeysetKeyStoreSnapshot;
import com.uid2.shared.store.reader.RotatingKeysetKeyStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class KeysetKeyParser implements Parser<KeysetKeyStoreSnapshot> {
    @Override
    public ParsingResult<KeysetKeyStoreSnapshot> deserialize(InputStream inputStream) throws IOException {
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, KeysetKey> keyIdToKeysetKey = new HashMap<>();
        final HashMap<Integer, List<KeysetKey>> keysetIdToKeysetKeyList = new HashMap<>();
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
            keyIdToKeysetKey.put(keysetKey.getId(), keysetKey);
            keysetIdToKeysetKeyList.computeIfAbsent(keysetId, k -> new ArrayList<>()).add(keysetKey);
        }
        KeysetKeyStoreSnapshot snapshot = new KeysetKeyStoreSnapshot(keyIdToKeysetKey, keysetIdToKeysetKeyList);
        return new ParsingResult<>(snapshot, keysSpec.size());
    }
}
