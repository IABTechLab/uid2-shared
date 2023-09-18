package com.uid2.shared.store.reader;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.Keyset;
import com.uid2.shared.auth.KeysetSnapshot;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.cloud.InMemoryStorageMock;
import com.uid2.shared.model.KeysetKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.scope.GlobalScope;
import com.uid2.shared.store.scope.SiteScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static com.uid2.shared.TestUtilites.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class RotatingKeysetProviderTest {
    private static final Instant NOW = Instant.now();

    private AutoCloseable mocks;
    @Mock private ICloudStorage cloudStorage;

    private RotatingKeysetProvider keysetProvider;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keysetProvider = new RotatingKeysetProvider(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @After
    public void teardown() throws Exception {
        mocks.close();
    }

    @Test
    public void loadsContentToSiteScope() throws Exception{
        RotatingKeysetProvider provider = new RotatingKeysetProvider(cloudStorage, new SiteScope(new CloudPath("metadata"), 5));

        JsonArray content = new JsonArray();
        addKeyset(content, 5, 1, List.of(1, 2, 3));
        addKeyset(content, 1, 2, List.of(1,2,5));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = provider.loadContent(makeMetadata("locationPath"));
        assertEquals(2, count);
    }

    @Test
    public void loadsContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keysetProvider.loadContent(makeMetadata("locationPath"));

        assertEquals(0, count);
    }

    @Test
    public void loadsContent() throws Exception {
        InMemoryStorageMock cloudStorage = new InMemoryStorageMock();
        String contentPath = "file.json";
        String metadataPath = "metadata.json";

        cloudStorage.upload(toInputStream(makeMetadata(contentPath).encodePrettily()), metadataPath);

        JsonArray content = new JsonArray();
        addKeyset(content, 1, 1, List.of(1, 2,3));
        cloudStorage.upload(toInputStream(content.encodePrettily()), contentPath);

        RotatingKeysetProvider provider = new RotatingKeysetProvider(cloudStorage, new GlobalScope(new CloudPath(metadataPath)));
        provider.loadContent();

        KeysetSnapshot snapshot = provider.getSnapshot();
        Map<Integer, Keyset> actual = snapshot.getAllKeysets();
        Set<Integer> expectedAllowList = new HashSet<>();
        expectedAllowList.add(1);
        expectedAllowList.add(2);
        expectedAllowList.add(3);

        assertAll(
                "loadsContent",
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(1).getAllowedSites()).isEqualTo(expectedAllowList)
        );
    }

    @Test
    public void loadsContentMultipleEntries() throws Exception {
        JsonArray content = new JsonArray();
        addKeyset(content, 1, 1, List.of(2,3));
        addKeyset(content, 2, 2, List.of(3,4));
        addKeyset(content, 3, 3, List.of(5,6));
        addKeyset(content, 4, 4, null);
        addKeyset(content, 5, 5, List.of(1,1,2,3));
        addKeyset(content, 6, 6, List.of());
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keysetProvider.loadContent(makeMetadata("locationPath"));

        assertAll(
                "loadsContentMultipleEntries",
                () -> assertEquals(6, count),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 2 can access site 1 and itself, but not site 3",
                        () -> assertTrue(canAccessKey(2, 1, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(2, 1, MissingAclMode.DENY_ALL)),
                        () -> assertTrue(canAccessKey(2, 2, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(2, 2, MissingAclMode.DENY_ALL)),
                        () -> assertFalse(canAccessKey(2, 3, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(2, 3, MissingAclMode.DENY_ALL))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - null list",
                        () -> assertTrue(canAccessKey(4,4, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(4,4, MissingAclMode.DENY_ALL)),
                        () -> assertTrue(canAccessKey(2, 4, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(2, 4, MissingAclMode.DENY_ALL)),
                        () -> assertTrue(canAccessKey(5, 4, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(5, 4, MissingAclMode.DENY_ALL))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - empty list",
                        () -> assertTrue(canAccessKey(6,6, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(6,6, MissingAclMode.DENY_ALL)),
                        () -> assertFalse(canAccessKey(5,6, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(5,6, MissingAclMode.DENY_ALL)),
                        () -> assertFalse(canAccessKey(4,6, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(4,6, MissingAclMode.DENY_ALL))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - list with duplicates",
                        () -> assertTrue(canAccessKey(1, 5, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(1, 5, MissingAclMode.DENY_ALL)),
                        () -> assertTrue(canAccessKey(2, 5, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(2, 5, MissingAclMode.DENY_ALL)),
                        () -> assertFalse(canAccessKey(4, 5, MissingAclMode.ALLOW_ALL)),
                        () -> assertFalse(canAccessKey(4, 5, MissingAclMode.DENY_ALL)),
                        () -> assertTrue(canAccessKey(5, 5, MissingAclMode.ALLOW_ALL)),
                        () -> assertTrue(canAccessKey(5, 5, MissingAclMode.DENY_ALL))
                )
        );
    }

    private void addKeyset(JsonArray content, int siteId, int keysetId, Collection<Integer> allowedSites) {
        JsonObject entry = new JsonObject();
        entry.put("keyset_id", keysetId);
        entry.put("site_id", siteId);
        entry.put("name", "test");

        if(allowedSites == null) {
            entry.put("allowed_sites", allowedSites);
        } else {
            JsonArray list = new JsonArray();
            for (int allowedSiteId : allowedSites) {
                list.add(allowedSiteId);
            }
            entry.put("allowed_sites", list);
        }

        entry.put("created", Instant.now().getEpochSecond());
        entry.put("enabled", true);
        entry.put("default", true);

        content.add(entry);
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keysets = new JsonObject();
        keysets.put("location", location);
        metadata.put("keysets", keysets);
        return metadata;
    }

    private ClientKey makeClientKey(int siteId) {
        return new ClientKey(
                "test-client-key",
                "test-client-keyHash",
                "test-client-keySalt",
                Utils.toBase64String("test-client-secret".getBytes(StandardCharsets.UTF_8)),
                "test-client-name",
                NOW,
                Set.of(),
                siteId
        );
    }

    private KeysetKey makeKey(int keysetId) {
        return new KeysetKey(0, null, null, null, null, keysetId);
    }

    private boolean canAccessKey(int clientSiteId, int keySiteId, MissingAclMode mode) {
        return keysetProvider.getSnapshot().canClientAccessKey(makeClientKey(clientSiteId), makeKey(keySiteId), mode);
    }
}
