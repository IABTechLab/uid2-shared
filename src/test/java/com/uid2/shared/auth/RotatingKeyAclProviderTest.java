package com.uid2.shared.auth;

import com.uid2.shared.Utils;
import com.uid2.shared.cloud.InMemoryStorageMock;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.cloud.ICloudStorage;

import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.reader.RotatingKeyAclProvider;
import com.uid2.shared.store.scope.GlobalScope;
import com.uid2.shared.store.scope.SiteScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static com.uid2.shared.TestUtilites.toInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RotatingKeyAclProviderTest {
    @Mock private ICloudStorage cloudStorage;

    private RotatingKeyAclProvider keyAclProvider;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        keyAclProvider = new RotatingKeyAclProvider(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject acls = new JsonObject();
        acls.put("location", location);
        metadata.put("keys_acl", acls);
        return metadata;
    }

    private void addBlacklist(JsonArray content, int siteId, int... blacklistedSiteIds) {
        addAccessList(content, siteId, "blacklist", blacklistedSiteIds);
    }

    private void addWhitelist(JsonArray content, int siteId, int... whitelistedSiteIds) {
        addAccessList(content, siteId, "whitelist", whitelistedSiteIds);
    }

    private void addAccessList(JsonArray content, int siteId, String listType, int... listedSiteIds) {
        JsonObject entry = new JsonObject();
        entry.put("site_id", siteId);

        JsonArray list = new JsonArray();
        for (int listedSiteId : listedSiteIds) {
            list.add(listedSiteId);
        }
        entry.put(listType, list);

        content.add(entry);
    }

    private ClientKey makeClientKey(int siteId) {
        return new ClientKey(
                "test-client-keyHash",
                "test-client-keySalt",
                Utils.toBase64String("test-client-secret".getBytes(StandardCharsets.UTF_8)),
                "test-client-name",
                Instant.MIN,
                Set.of(),
                siteId,
                "test-key-id"
        );
    }

    private EncryptionKey makeKey(int siteId) {
        return new EncryptionKey(0, null, null, null, null, siteId);
    }

    private boolean canAccessKey(int clientSiteId, int keySiteId) {
        return keyAclProvider.getSnapshot().canClientAccessKey(makeClientKey(clientSiteId), makeKey(keySiteId));
    }

    @Test
    public void loadsContentToSiteScope() throws Exception {
        RotatingKeyAclProvider provider = new RotatingKeyAclProvider(cloudStorage, new SiteScope(new CloudPath("metadata"), 5));

        JsonArray content = new JsonArray();
        addBlacklist(content, 1, 2, 3);
        addWhitelist(content, 3, 4, 5);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = provider.loadContent(makeMetadata("locationPath"));

        assertEquals(2, count);
    }

    @Test public void loadsContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyAclProvider.loadContent(makeMetadata("locationPath"));

        assertEquals(0, count);
    }

    @Test
    public void loadsContent() throws Exception {
        InMemoryStorageMock cloudStorage = new InMemoryStorageMock();
        String contentPath = "file.json";
        String metadataPath = "metadata.json";
        int siteId = 1;

        cloudStorage.upload(toInputStream(makeMetadata(contentPath).encodePrettily()), metadataPath);

        JsonArray content = new JsonArray();
        addBlacklist(content, siteId, 2, 3);
        cloudStorage.upload(toInputStream(content.encodePrettily()), contentPath);

        RotatingKeyAclProvider provider = new RotatingKeyAclProvider(cloudStorage, new GlobalScope(new CloudPath(metadataPath)));
        provider.loadContent();

        AclSnapshot snapshot = provider.getSnapshot();
        Map<Integer, EncryptionKeyAcl> actual = snapshot.getAllAcls();
        Set<Integer> expectedBlacklist = new HashSet<>();
        expectedBlacklist.add(2);
        expectedBlacklist.add(3);

        assertAll(
                "loadsContent",
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(siteId).getAccessList()).isEqualTo(expectedBlacklist),
                () -> assertThat(actual.get(siteId).getIsWhitelist()).isEqualTo(false)
        );
    }

    @Test public void loadsContentMultipleEntries() throws Exception {
        JsonArray content = new JsonArray();
        addBlacklist(content,1, 2, 3);
        addBlacklist(content, 2, 4, 6, 3);
        addWhitelist(content, 3, 2, 4);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        long count = keyAclProvider.loadContent(makeMetadata("locationPath"));

        assertAll(
                "loadsContentMultipleEntries",
                () -> assertEquals(3, count),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 1 is not blacklisted",
                        () -> assertTrue(canAccessKey(1, 1)),
                        () -> assertTrue(canAccessKey(1, 2)),
                        () -> assertFalse(canAccessKey(1, 3)),
                        () -> assertTrue(canAccessKey(1, 4)),
                        () -> assertTrue(canAccessKey(1, 5)),
                        () -> assertTrue(canAccessKey(1, 6))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 2 is blacklisted by site 1 and whitelisted by site 3",
                        () -> assertFalse(canAccessKey(2, 1)),
                        () -> assertTrue(canAccessKey(2, 2)),
                        () -> assertTrue(canAccessKey(2, 3)),
                        () -> assertTrue(canAccessKey(2, 4)),
                        () -> assertTrue(canAccessKey(2, 5)),
                        () -> assertTrue(canAccessKey(2, 6))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 3 is blacklisted by site 1 and site 2",
                        () -> assertFalse(canAccessKey(3, 1)),
                        () -> assertFalse(canAccessKey(3, 2)),
                        () -> assertTrue(canAccessKey(3, 3)),
                        () -> assertTrue(canAccessKey(3, 4)),
                        () -> assertTrue(canAccessKey(3, 5)),
                        () -> assertTrue(canAccessKey(3, 6))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 4 is blacklisted by site 2 and whitelisted by site 3",
                        () -> assertTrue(canAccessKey(4, 1)),
                        () -> assertFalse(canAccessKey(4, 2)),
                        () -> assertTrue(canAccessKey(4, 3)),
                        () -> assertTrue(canAccessKey(4, 4)),
                        () -> assertTrue(canAccessKey(4, 5)),
                        () -> assertTrue(canAccessKey(4, 6))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 5 is not blacklisted by any sites as it is not in any ACL lists",
                        () -> assertTrue(canAccessKey(5, 1)),
                        () -> assertTrue(canAccessKey(5, 2)),
                        () -> assertFalse(canAccessKey(5, 3)),
                        () -> assertTrue(canAccessKey(5, 4)),
                        () -> assertTrue(canAccessKey(5, 5)),
                        () -> assertTrue(canAccessKey(5, 6))
                ),
                () -> assertAll(
                        "loadsContentMultipleEntries - site 6 is blacklisted by site 2",
                        () -> assertTrue(canAccessKey(6, 1)),
                        () -> assertFalse(canAccessKey(6, 2)),
                        () -> assertFalse(canAccessKey(6, 3)),
                        () -> assertTrue(canAccessKey(6, 4)),
                        () -> assertTrue(canAccessKey(6, 5)),
                        () -> assertTrue(canAccessKey(6, 6))
                )
        );
    }
}
