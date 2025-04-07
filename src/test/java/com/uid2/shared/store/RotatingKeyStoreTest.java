package com.uid2.shared.store;

import com.uid2.shared.Const;
import com.uid2.shared.model.EncryptionKey;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.reader.RotatingKeyStore;
import com.uid2.shared.store.scope.GlobalScope;
import static com.uid2.shared.TestUtilites.makeInputStream;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RotatingKeyStoreTest {
    @Mock
    private ICloudStorage cloudStorage;
    private RotatingKeyStore keyStore;

    @BeforeEach
    public void setup() {
        keyStore = new RotatingKeyStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keys = new JsonObject();
        keys.put("location", location);
        metadata.put("keys", keys);
        return metadata;
    }

    private void addKey(JsonArray content, int keyId, Integer siteId, String secret, Instant activates) {
        Instant expires = activates.plusSeconds(1000);
        addKey(content, keyId, siteId, secret, activates, expires);
    }

    private void addKey(JsonArray content, int keyId, Integer siteId, String secret, Instant activates, Instant expires) {
        JsonObject key = new JsonObject();
        key.put("id", keyId);
        if(siteId != null) {
            key.put("site_id", siteId);
        }
        key.put("created", activates.minusSeconds(1000).getEpochSecond());
        key.put("activates", activates.getEpochSecond());
        key.put("expires", expires.getEpochSecond());
        key.put("secret", secret.getBytes());
        content.add(key);
    }

    private void checkKeyMatches(EncryptionKey key, int keyId, int siteId, String secret) {
        assertNotNull(key);
        assertEquals(keyId, key.getId());
        assertEquals(siteId, key.getSiteId());
        assertArrayEquals(secret.getBytes(), key.getKeyBytes());
    }

    private void checkActiveKeySetEquals(Integer... expectedKeyIds) {
        List<Integer> actualKeyIds = keyStore.getSnapshot().getActiveKeySet().stream().map((key) -> key.getId()).collect(Collectors.toList());
        assertArrayEquals(expectedKeyIds, actualKeyIds.toArray(new Integer[0]));
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyStore.loadContent(makeMetadata("locationPath"));

        assertEquals(0, count);
        assertNull(keyStore.getSnapshot().getMasterKey(Instant.now()));
        assertNull(keyStore.getSnapshot().getKey(1));
        assertNull(keyStore.getSnapshot().getActiveSiteKey(2, Instant.now()));
        assertTrue(keyStore.getSnapshot().getActiveKeySet().isEmpty());
    }

    @Test
    public void loadContentMultipleKeysArray() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, Const.Data.MasterKeySiteId, "system", now);
        addKey(content, 102, 202, "key102site202", now);
        addKey(content, 103, 203, "key103site203", now);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyStore.loadContent(makeMetadata("locationPath"));

        assertEquals(3, count);
        checkKeyMatches(keyStore.getSnapshot().getMasterKey(now), 101, -1, "system");
        assertNull(keyStore.getSnapshot().getKey(1));
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, -1, "system");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, 202, "key102site202");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, 203, "key103site203");
        assertNull(keyStore.getSnapshot().getActiveSiteKey(2, now));
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(202, now), 102, 202, "key102site202");
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(203, now), 103, 203, "key103site203");
        checkActiveKeySetEquals(101, 102, 103);
    }

    @Test
    public void loadContentMultipleKeysForSameSite() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, 200, "key101site200", now.minusSeconds(50), now.plusSeconds(30));
        addKey(content, 102, 200, "key102site200", now.plusSeconds(1), now.plusSeconds(50)); // not active yet
        addKey(content, 103, 200, "key103site200", now.minusSeconds(10), now.plusSeconds(20));
        addKey(content, 104, 200, "key104site200", now.minusSeconds(5), now.minusSeconds(1)); // expired
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyStore.loadContent(makeMetadata("locationPath"));

        assertEquals(4, count);
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, 200, "key101site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, 200, "key102site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, 200, "key103site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(104), 104, 200, "key104site200");
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(200, now), 103, 200, "key103site200");
        checkActiveKeySetEquals(101, 102, 103, 104);
    }

    @Test
    public void loadContentMultipleSystemKeys() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, Const.Data.MasterKeySiteId, "system1", now.plusSeconds(1));
        addKey(content, 102, Const.Data.MasterKeySiteId, "system2", now);
        addKey(content, 103, Const.Data.MasterKeySiteId, "system3", now.minusSeconds(1));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyStore.loadContent(makeMetadata("locationPath"));

        assertEquals(3, count);
        checkKeyMatches(keyStore.getSnapshot().getMasterKey(now), 102, -1, "system2");
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, -1, "system1");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, -1, "system2");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, -1, "system3");
        checkActiveKeySetEquals(101, 102, 103);
    }
}
