package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.ClientSideKeypair;
import com.uid2.shared.store.ClientSideKeypairStoreSnapshot;
import com.uid2.shared.store.IClientSideKeypairStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class ClientSideKeypairParser implements Parser<IClientSideKeypairStore.IClientSideKeypairStoreSnapshot> {
    @Override
    public ParsingResult<IClientSideKeypairStore.IClientSideKeypairStoreSnapshot> deserialize(InputStream inputStream) throws IOException {
        JsonArray keypairsSpec = Utils.toJsonArray(inputStream);
        final HashMap<String, ClientSideKeypair> keypairMap = new HashMap<>();
        final HashMap<Integer, List<ClientSideKeypair>> siteKeypairMap = new HashMap<>();
        for (int i = 0; i < keypairsSpec.size(); i++){
            JsonObject pairSpec = keypairsSpec.getJsonObject(i);
            String subscriptionId = pairSpec.getString("subscription_id");
            int siteId = pairSpec.getInteger("site_id");
            String contact = pairSpec.getString("contact");
            boolean disabled = pairSpec.getBoolean("disabled");
            Instant created = Instant.ofEpochSecond(pairSpec.getLong("created"));
            ClientSideKeypair keypair = new ClientSideKeypair(
                    subscriptionId,
                    Base64.getDecoder().decode(pairSpec.getString("public_key")),
                    Base64.getDecoder().decode(pairSpec.getString("private_key")),
                    siteId,
                    contact,
                    created,
                    disabled
            );
            keypairMap.put(subscriptionId, keypair);
            siteKeypairMap.computeIfAbsent(siteId, id -> new ArrayList<>());
            siteKeypairMap.get(siteId).add(keypair);
        }
        ClientSideKeypairStoreSnapshot snapshot = new ClientSideKeypairStoreSnapshot(keypairMap, siteKeypairMap);
        return new ParsingResult<>(snapshot, keypairsSpec.size());
    }
}
