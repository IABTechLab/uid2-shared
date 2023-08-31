package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.ClientKey;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClientParser implements Parser<Map<String, ClientKey>> {
    @Override
    public ParsingResult<Map<String, ClientKey>> deserialize(InputStream inputStream) throws IOException {
        JsonArray keysSpec = Utils.toJsonArray(inputStream);
        Map<String, ClientKey> keyMap = new HashMap<>();
        for (int i = 0; i < keysSpec.size(); ++i) {
            JsonObject keySpec = keysSpec.getJsonObject(i);
            ClientKey clientKey = ClientKey.valueOf(keySpec);
            keyMap.put(clientKey.getKey(), clientKey);
        }
        return new ParsingResult<>(keyMap, keysSpec.size());
    }
}
