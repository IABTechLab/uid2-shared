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
import java.util.*;

public class ClientSideKeypairParser implements Parser<IClientSideKeypairStore.IClientSideKeypairStoreSnapshot> {
    @Override
    public ParsingResult<IClientSideKeypairStore.IClientSideKeypairStoreSnapshot> deserialize(InputStream inputStream) throws IOException {
        final JsonArray keypairsSpec = Utils.toJsonArray(inputStream);

        final HashMap<String, ClientSideKeypair> keypairMap = new HashMap<>();
        final HashMap<Integer, List<ClientSideKeypair>> siteKeypairMap = new HashMap<>();

        for (int i = 0; i < keypairsSpec.size(); i++) {
            final JsonObject pairSpec = keypairsSpec.getJsonObject(i);

            final String subscriptionId = pairSpec.getString("subscription_id");
            final int siteId = pairSpec.getInteger("site_id");
            final String contact = pairSpec.getString("contact");
            final boolean disabled = pairSpec.getBoolean("disabled");
            final String name = pairSpec.getString("name", "");
            final Instant created = Instant.ofEpochSecond(pairSpec.getLong("created"));

            final ClientSideKeypair keypair = new ClientSideKeypair(
                    subscriptionId,
                    pairSpec.getString("public_key"),
                    pairSpec.getString("private_key"),
                    siteId,
                    contact,
                    created,
                    disabled,
                    name
            );

            keypairMap.put(subscriptionId, keypair);
            siteKeypairMap.computeIfAbsent(siteId, id -> new ArrayList<>()).add(keypair);
        }

        final ClientSideKeypairStoreSnapshot snapshot = new ClientSideKeypairStoreSnapshot(keypairMap, siteKeypairMap);
        return new ParsingResult<>(snapshot, keypairsSpec.size());
    }
}
