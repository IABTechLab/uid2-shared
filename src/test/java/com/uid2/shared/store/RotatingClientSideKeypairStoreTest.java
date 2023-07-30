package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.ClientSideKeypair;
import com.uid2.shared.store.reader.RotatingClientSideKeypairStore;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.mockito.Mockito.when;

public class RotatingClientSideKeypairStoreTest {
    private AutoCloseable mocks;
    @Mock
    ICloudStorage cloudStorage;
    private RotatingClientSideKeypairStore keypairStore;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keypairStore = new RotatingClientSideKeypairStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @After
    public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keypairs = new JsonObject();
        keypairs.put("location", location);
        metadata.put("client_side_keypairs", keypairs);
        return metadata;
    }

    private ClientSideKeypair addKeypair(JsonArray content, String subscriptionId, String publicKey, String privateKey, Integer siteId, String contact, Instant created, boolean disabled) {
        long created_secs = created.getEpochSecond();

        ClientSideKeypair k = new ClientSideKeypair(subscriptionId, publicKey.getBytes(), privateKey.getBytes(), siteId, contact, Instant.ofEpochSecond(created_secs), disabled, "UID2-X-T-", "UID2-Y-T-");

        JsonObject keypair = new JsonObject();
        keypair.put("subscription_id", subscriptionId);
        keypair.put("public_key", k.encodePublicKeyToString());
        keypair.put("private_key", k.encodePrivateKeyToString());
        keypair.put("site_id", siteId);
        keypair.put("contact", contact);
        keypair.put("created", created.getEpochSecond());
        keypair.put("disabled", disabled);
        content.add(keypair);
        return k;
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keypairStore.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(0, count);
        Assert.assertNull(keypairStore.getSnapshot().getKeypair("test-subscription-id"));
        Assert.assertTrue(keypairStore.getSnapshot().getAll().isEmpty());
        Assert.assertTrue(keypairStore.getAll().isEmpty());
        Assert.assertNull(keypairStore.getSnapshot().getSiteKeypairs(25));
        Assert.assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().isEmpty());
    }

    @Test
    public void loadContentMultipleKeys() throws Exception {
        JsonArray content = new JsonArray();
        ClientSideKeypair keypair1 = addKeypair(content, "id-1", "pub1", "priv1", 1, "email1@email.com", Instant.now(), false);
        ClientSideKeypair keypair2 = addKeypair(content, "id-2", "pub2", "priv2", 2, "email2@email.com", Instant.now(), false);
        ClientSideKeypair keypair3 = addKeypair(content, "id-3", "pub3", "priv3", 3, "email3@email.com", Instant.now(), true);
        ClientSideKeypair keypair4 = addKeypair(content, "id-4", "pub4", "priv4", 3, "email3@email.com", Instant.now(), true);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = keypairStore.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(4, count);

        Assert.assertTrue(keypairStore.getSnapshot().getKeypair("id-1").equals(keypair1));
        Assert.assertTrue(keypairStore.getSnapshot().getKeypair("id-2").equals(keypair2));
        Assert.assertTrue(keypairStore.getSnapshot().getKeypair("id-3").equals(keypair3));
        Assert.assertTrue(keypairStore.getSnapshot().getKeypair("id-4").equals(keypair4));

        Assert.assertEquals(1, keypairStore.getSnapshot().getSiteKeypairs(1).size());
        Assert.assertEquals(1, keypairStore.getSnapshot().getSiteKeypairs(2).size());
        Assert.assertEquals(2, keypairStore.getSnapshot().getSiteKeypairs(3).size());

        Assert.assertTrue(keypairStore.getSnapshot().getSiteKeypairs(1).contains(keypair1));
        Assert.assertTrue(keypairStore.getSnapshot().getSiteKeypairs(2).contains(keypair2));
        Assert.assertTrue(keypairStore.getSnapshot().getSiteKeypairs(3).contains(keypair3));
        Assert.assertTrue(keypairStore.getSnapshot().getSiteKeypairs(3).contains(keypair4));

        Assert.assertEquals(2, keypairStore.getSnapshot().getEnabledKeypairs().size());
        Assert.assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().contains(keypair1));
        Assert.assertTrue(keypairStore.getSnapshot().getEnabledKeypairs().contains(keypair2));
    }

}
