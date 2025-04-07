package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.ClientSideKeypair;
import com.uid2.shared.store.reader.RotatingClientSideKeypairStore;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RotatingClientSideKeypairStoreTest {
    @Mock
    private ICloudStorage cloudStorage;
    private RotatingClientSideKeypairStore keypairStore;

    @BeforeEach
    public void setup() {
        keypairStore = new RotatingClientSideKeypairStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keypairs = new JsonObject();
        keypairs.put("location", location);
        metadata.put("client_side_keypairs", keypairs);
        return metadata;
    }

    private ClientSideKeypair addKeypair(JsonArray content, String subscriptionId, String publicKey, String privateKey, Integer siteId, String contact, Instant created, boolean disabled, String name) {
        long created_secs = created.getEpochSecond();

        ClientSideKeypair k = new ClientSideKeypair(subscriptionId, "UID2-X-T-" + publicKey, "UID2-Y-T-" + privateKey, siteId, contact, Instant.ofEpochSecond(created_secs), disabled, name);

        JsonObject keypair = new JsonObject();
        keypair.put("subscription_id", subscriptionId);
        keypair.put("public_key", k.encodePublicKeyToString());
        keypair.put("private_key", k.encodePrivateKeyToString());
        keypair.put("site_id", siteId);
        keypair.put("contact", contact);
        keypair.put("created", created.getEpochSecond());
        keypair.put("disabled", disabled);
        keypair.put("name", name);
        content.add(keypair);
        return k;
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = keypairStore.loadContent(makeMetadata("locationPath"));

        assertEquals(0, count);
        assertNull(keypairStore.getSnapshot().getKeypair("test-subscription-id"));
        assertTrue(keypairStore.getSnapshot().getAll().isEmpty());
        assertTrue(keypairStore.getAll().isEmpty());
        assertNull(keypairStore.getSnapshot().getSiteKeypairs(25));
        assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().isEmpty());
    }

    @Test
    public void loadContentMultipleKeys() throws Exception {
        JsonArray content = new JsonArray();
        ClientSideKeypair keypair1 = addKeypair(content, "id-1", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEhQ9i767j9beaz8sUhxkgrnW38gIUgG07+8+4ubb80NnikzLhVE7ZHd22haNF6iNNu8O7t7h21IizIifRkCC8OQ==", "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAtmOklUGeCTv9XRp9cS9PIZAKW3bcntTVtzewaFw9/2A==", 1, "email1@email.com", Instant.now(), false, "name 1");
        ClientSideKeypair keypair2 = addKeypair(content, "id-2", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE+igludojFNfaFcidrG13OdO8NnzMv6DfqCogaEP1JoQ/ciOA4RLx4djje8BtXddafFMPU8nG5qMomTSg67Lp+A==", "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAshNg/7jgVzpyueRlF73Y4YvH18P+4EUed5Pw5ZAbnqA==", 2, "email2@email.com", Instant.now(), false, "name 2");
        ClientSideKeypair keypair3 = addKeypair(content, "id-3", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEoy42kazyAedMNvXoakdZWAMqbkr2TICCsAJzOpOtbYbxwsJgAFJso9NCJTSsvpb0ChivMkA6mesicVlGdLy1ng==", "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCBt5EM8QQfaegeYWzxbFTkn+HRZmZ3kR0Eqeesv6aMHMA==", 3, "email3@email.com", Instant.now(), true, "name 3");
        ClientSideKeypair keypair4 = addKeypair(content, "id-4", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEP5F7PslSFDWcTgasIc1x6183/JqI8WGOqXYxV2n7F6fAdZe8jLVvYtNhub2R+ZfXIDwdDepEZkuNSxfgwM27GA==", "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDe6TIHd+Eyoczq1a8xeNGw17OWjeJHZwSLXtuMcqCXZQ==", 3, "email3@email.com", Instant.now(), true, "name 4");
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = keypairStore.loadContent(makeMetadata("locationPath"));

        assertEquals(4, count);
        assertEquals(keypair1, keypairStore.getSnapshot().getKeypair("id-1"));
        assertEquals(keypair2, keypairStore.getSnapshot().getKeypair("id-2"));
        assertEquals(keypair3, keypairStore.getSnapshot().getKeypair("id-3"));
        assertEquals(keypair4, keypairStore.getSnapshot().getKeypair("id-4"));

        assertEquals(1, keypairStore.getSnapshot().getSiteKeypairs(1).size());
        assertEquals(1, keypairStore.getSnapshot().getSiteKeypairs(2).size());
        assertEquals(2, keypairStore.getSnapshot().getSiteKeypairs(3).size());

        assertTrue(keypairStore.getSnapshot().getSiteKeypairs(1).contains(keypair1));
        assertTrue(keypairStore.getSnapshot().getSiteKeypairs(2).contains(keypair2));
        assertTrue(keypairStore.getSnapshot().getSiteKeypairs(3).contains(keypair3));
        assertTrue(keypairStore.getSnapshot().getSiteKeypairs(3).contains(keypair4));

        assertEquals(2, keypairStore.getSnapshot().getEnabledKeypairs().size());
        assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().contains(keypair1));
        assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().contains(keypair2));
    }
}
