package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.KeyStoreSnapshot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static com.uid2.shared.store.IKeyStore.*;

public class KeyParser implements Parser<IKeyStoreSnapshot> {
    public ParsingResult<IKeyStoreSnapshot> deserialize(InputStream inputStream) throws IOException {
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, EncryptionKey> keyMap = new HashMap<>();
        final HashMap<Integer, List<EncryptionKey>> siteKeyMap = new HashMap<>();
        for (int i = 0; i < keysSpec.size(); ++i) {
            JsonObject keySpec = keysSpec.getJsonObject(i);
            int siteId = keySpec.getInteger("site_id");
            Instant created = Instant.ofEpochSecond(keySpec.getLong("created"));
            Instant activates = Instant.ofEpochSecond(keySpec.getLong("activates"));
            Instant expires = Instant.ofEpochSecond(keySpec.getLong("expires"));
            EncryptionKey key = new EncryptionKey(
                    keySpec.getInteger("id"),
                    Base64.getDecoder().decode(keySpec.getString("secret")),
                    created, activates, expires, siteId);
            keyMap.put(key.getId(), key);
            siteKeyMap.computeIfAbsent(siteId, k -> new ArrayList<>()).add(key);
        }
        KeyStoreSnapshot snapshot = new KeyStoreSnapshot(keyMap, siteKeyMap);
        return new ParsingResult<>(snapshot, keysSpec.size());
    }
}
