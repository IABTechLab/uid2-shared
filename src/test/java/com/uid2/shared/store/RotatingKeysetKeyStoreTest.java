package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.reader.RotatingKeysetKeyStore;
import com.uid2.shared.store.scope.GlobalScope;
import static com.uid2.shared.TestUtilites.makeInputStream;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.Mockito.when;

public class RotatingKeysetKeyStoreTest {
    private AutoCloseable mocks;
    @Mock
    ICloudStorage cloudStorage;
    private RotatingKeysetKeyStore keysetKeyStore;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keysetKeyStore = new RotatingKeysetKeyStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @After
    public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keys = new JsonObject();
        keys.put("location", location);
        metadata.put("keyset_keys", keys);
        return metadata;
    }

    private KeysetKey addKey(JsonArray content, int keyId, Integer keysetId, String secret, Instant activates) {
        Instant expires = activates.plusSeconds(1000);
        return addKey(content, keyId, keysetId, secret, activates, expires);
    }

    private KeysetKey addKey(JsonArray content, int keyId, Integer keysetId, String secret, Instant activates, Instant expires) {
        long created_secs = activates.minusSeconds(1000).getEpochSecond();
        long activates_secs = activates.getEpochSecond();
        long expires_secs = expires.getEpochSecond();
        JsonObject key = new JsonObject();
        key.put("id", keyId);
        key.put("keyset_id", keysetId);
        key.put("created", created_secs);
        key.put("activates", activates_secs);
        key.put("expires", expires_secs);
        key.put("secret", secret.getBytes());
        content.add(key);
        return new KeysetKey(keyId, secret.getBytes(), Instant.ofEpochSecond(created_secs), Instant.ofEpochSecond(activates_secs), Instant.ofEpochSecond(expires_secs), keysetId);
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keysetKeyStore.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(0, count);
        Assert.assertNull(keysetKeyStore.getSnapshot().getKey(1));
        Assert.assertNull(keysetKeyStore.getSnapshot().getActiveKey(1, Instant.now()));
        Assert.assertTrue(keysetKeyStore.getSnapshot().getActiveKeysetKeys().isEmpty());
    }

    @Test
    public void loadContentMultipleKeys() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        KeysetKey key1 = addKey(content, 101, 1, "testsecret1", now);
        KeysetKey key2 = addKey(content, 102, 2, "testsecret2", now);
        KeysetKey key3 = addKey(content, 103, 3, "testsecret3", now);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long conunt = keysetKeyStore.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(3, conunt);
        Assert.assertTrue(keysetKeyStore.getSnapshot().getKey(101).equals(key1));
        Assert.assertTrue(keysetKeyStore.getSnapshot().getKey(102).equals(key2));
        Assert.assertTrue(keysetKeyStore.getSnapshot().getKey(103).equals(key3));
    }
    @Test
    public void loadContentMultipleFromSingleKeyset() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        KeysetKey key1 = addKey(content, 101, 200, "key101set200", now.minusSeconds(50), now.plusSeconds(30));
        KeysetKey key2 = addKey(content, 102, 200, "key102set200", now.plusSeconds(1), now.plusSeconds(50)); // not active yet
        KeysetKey key3 = addKey(content, 103, 200, "key103set200", now.minusSeconds(10), now.plusSeconds(20));
        KeysetKey key4 = addKey(content, 104, 200, "key104set200", now.minusSeconds(5), now.minusSeconds(1)); // expired
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keysetKeyStore.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(4, count);
        Assert.assertEquals(keysetKeyStore.getSnapshot().getKey(101), key1);
        Assert.assertEquals(keysetKeyStore.getSnapshot().getKey(102), key2);
        Assert.assertEquals(keysetKeyStore.getSnapshot().getKey(103), key3);
        Assert.assertEquals(keysetKeyStore.getSnapshot().getKey(104), key4);
        //Check active key is correct
        Assert.assertEquals(keysetKeyStore.getSnapshot().getActiveKey(200, now), key3);
        Assert.assertEquals(keysetKeyStore.getSnapshot().getActiveKey(200, keysetKeyStore.getSnapshot().getKey(103).getActivates()), key3);
    }
}
