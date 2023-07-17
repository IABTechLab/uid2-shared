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
import org.junit.Assert;
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
import static org.mockito.Mockito.when;

public class RotatingKeysetProviderTest {
    private AutoCloseable mocks;
    @Mock private ICloudStorage cloudStorage;
    private RotatingKeysetProvider keysetProvider;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keysetProvider = new RotatingKeysetProvider(cloudStorage, new GlobalScope(new CloudPath("metadata")));
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

    @Test
    public void loadsContentToSiteScope() throws Exception{
        RotatingKeysetProvider provider = new RotatingKeysetProvider(cloudStorage, new SiteScope(new CloudPath("metadata"), 5));
        JsonArray content = new JsonArray();
        addKeyset(content, 5, 1, List.of(1, 2, 3));
        addKeyset(content, 1, 2, List.of(1,2,5));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = provider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(2, count);
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keysetProvider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(0, count);
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
        assertThat(actual).hasSize(1);
        assertThat(actual.get(1).getAllowedSites()).isEqualTo(expectedAllowList);

    }
    private ClientKey makeClientKey(int siteId) {
        return new ClientKey("test-client-key", Utils.toBase64String("test-client-secret".getBytes(StandardCharsets.UTF_8))).withSiteId(siteId);
    }
    private KeysetKey makeKey(int keysetId) {
        return new KeysetKey(0, null, null, null, null, keysetId);
    }
    private boolean canAccessKey(int clientSiteId, int keySiteId, MissingAclMode mode) {
        return keysetProvider.getSnapshot().canClientAccessKey(makeClientKey(clientSiteId), makeKey(keySiteId), mode);
    }

    @Test
    public void loadContentMultipleEntries() throws Exception {
        JsonArray content = new JsonArray();
        addKeyset(content, 1, 1, List.of(2,3));
        addKeyset(content, 2, 2, List.of(3,4));
        addKeyset(content, 3, 3, List.of(5,6));
        addKeyset(content, 4, 4, null);
        addKeyset(content, 5, 5, List.of(1,1,2,3));
        addKeyset(content, 6, 6, List.of());
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keysetProvider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(6, count);

        // Site 2 can access 1, it can access its own, but not 3
        Assert.assertTrue(canAccessKey(2, 1, MissingAclMode.ALLOW_ALL));
        Assert.assertTrue(canAccessKey(2, 1, MissingAclMode.DENY_ALL));
        Assert.assertTrue(canAccessKey(2, 2, MissingAclMode.ALLOW_ALL));
        Assert.assertTrue(canAccessKey(2, 2, MissingAclMode.DENY_ALL));
        Assert.assertFalse(canAccessKey(2, 3, MissingAclMode.ALLOW_ALL));
        Assert.assertFalse(canAccessKey(2, 3, MissingAclMode.DENY_ALL));
        //Only 4 can access null list
        Assert.assertTrue(canAccessKey(4,4, MissingAclMode.ALLOW_ALL));
        Assert.assertTrue(canAccessKey(4,4, MissingAclMode.DENY_ALL));
        Assert.assertTrue(canAccessKey(2, 4, MissingAclMode.ALLOW_ALL));
        Assert.assertFalse(canAccessKey(2, 4, MissingAclMode.DENY_ALL));
        //Can still access if there is a duplicate
        Assert.assertTrue(canAccessKey(1, 5, MissingAclMode.ALLOW_ALL));
        Assert.assertTrue(canAccessKey(1, 5, MissingAclMode.DENY_ALL));
        // Only 6 can access its empty list
        Assert.assertTrue(canAccessKey(6,6, MissingAclMode.ALLOW_ALL));
        Assert.assertTrue(canAccessKey(6,6, MissingAclMode.DENY_ALL));
        Assert.assertTrue(canAccessKey(5,6, MissingAclMode.ALLOW_ALL));
        Assert.assertFalse(canAccessKey(5,6, MissingAclMode.DENY_ALL));
    }
}
