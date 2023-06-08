package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.Keyset;
import com.uid2.shared.auth.KeysetSnapshot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;

public class KeysetParser implements Parser<KeysetSnapshot> {
    @Override
    public ParsingResult<KeysetSnapshot> deserialize(InputStream inputStream) throws IOException {
        final JsonArray keysetsSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, Keyset> keysetMap = new HashMap<>();
        for(int i = 0; i < keysetsSpec.size(); i++) {
            final JsonObject keysetSpec = keysetsSpec.getJsonObject(i);
            final Integer keysetId = keysetSpec.getInteger("keyset_id");
            final Integer siteId = keysetSpec.getInteger("site_id");
            final String name = keysetSpec.getString("name");

            final JsonArray allowedSitesSpec = keysetSpec.getJsonArray("allowed_sites");
            final HashSet<Integer> allowedSites = new HashSet<>();
            for(int j = 0; j < allowedSitesSpec.size(); j++) {
                allowedSites.add(allowedSitesSpec.getInteger(j));
            }

            long created = keysetSpec.getLong("created");
            final boolean enabled = keysetSpec.getBoolean("enabled");
            final boolean isDefault = keysetSpec.getBoolean("default");

            keysetMap.put(keysetId, new Keyset(keysetId, siteId, name, allowedSites, created, enabled, isDefault));
        }
        return new ParsingResult<>(new KeysetSnapshot(keysetMap), keysetsSpec.size());
    }
}
