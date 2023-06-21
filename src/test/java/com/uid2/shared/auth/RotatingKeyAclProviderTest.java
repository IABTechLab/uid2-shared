package com.uid2.shared.auth;

import com.google.api.client.util.Sets;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static com.uid2.shared.TestUtilites.toInputStream;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RotatingKeyAclProviderTest {
    private AutoCloseable mocks;
    @Mock private ICloudStorage cloudStorage;
    private RotatingKeyAclProvider keyAclProvider;

    @Before public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keyAclProvider = new RotatingKeyAclProvider(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @After public void teardown() throws Exception {
        mocks.close();
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
        return new ClientKey("test-client-key", Utils.toBase64String("test-client-secret".getBytes(StandardCharsets.UTF_8))).withSiteId(siteId);
    }

    private EncryptionKey makeKey(int siteId) {
        return new EncryptionKey(0, null, null, null, null, siteId);
    }

    private boolean canAccessKey(int clientSiteId, int keySiteId) {
        return keyAclProvider.getSnapshot().canClientAccessKey(makeClientKey(clientSiteId), makeKey(keySiteId));
    }


    @Test public void loadsContentToSiteScope() throws Exception {
        RotatingKeyAclProvider provider = new RotatingKeyAclProvider(cloudStorage, new SiteScope(new CloudPath("metadata"), 5));
        JsonArray content = new JsonArray();
        addBlacklist(content, 1, 2, 3);
        addWhitelist(content, 3, 4, 5);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = provider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(2, count);
    }

    @Test public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyAclProvider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(0, count);
    }

    @Test public void loadsContent() throws Exception {
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
        assertThat(actual).hasSize(1);
        assertThat(actual.get(siteId).getAccessList()).isEqualTo(expectedBlacklist);
        assertThat(actual.get(siteId).getIsWhitelist()).isEqualTo(false);
    }

    @Test public void loadContentMultipleEntries() throws Exception {
        JsonArray content = new JsonArray();
        addBlacklist(content,1, 2, 3);
        addBlacklist(content, 2, 4, 6, 3);
        addWhitelist(content, 3, 2, 4);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyAclProvider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(3, count);

        // site id 1 is not blacklisted by anyone
        Assert.assertTrue(canAccessKey(1, 1));
        Assert.assertTrue(canAccessKey(1, 2));
        Assert.assertFalse(canAccessKey(1, 3));
        Assert.assertTrue(canAccessKey(1, 4));
        Assert.assertTrue(canAccessKey(1, 5));
        Assert.assertTrue(canAccessKey(1, 6));

        // site id 2 is blacklisted by site 1 and whitelisted by 3
        Assert.assertFalse(canAccessKey(2, 1));
        Assert.assertTrue(canAccessKey(2, 2));
        Assert.assertTrue(canAccessKey(2, 3));
        Assert.assertTrue(canAccessKey(2, 4));
        Assert.assertTrue(canAccessKey(2, 5));
        Assert.assertTrue(canAccessKey(2, 6));

        // site id 3 is blacklisted by both sites 1 and 2
        Assert.assertFalse(canAccessKey(3, 1));
        Assert.assertFalse(canAccessKey(3, 2));
        Assert.assertTrue(canAccessKey(3, 3));
        Assert.assertTrue(canAccessKey(3, 4));
        Assert.assertTrue(canAccessKey(3, 5));
        Assert.assertTrue(canAccessKey(3, 6));

        // site id 4 is blacklisted by site 2 and whitelisted by 3
        Assert.assertTrue(canAccessKey(4, 1));
        Assert.assertFalse(canAccessKey(4, 2));
        Assert.assertTrue(canAccessKey(4, 3));
        Assert.assertTrue(canAccessKey(4, 4));
        Assert.assertTrue(canAccessKey(4, 5));
        Assert.assertTrue(canAccessKey(4, 6));

        // site id 5 is not mentioned anywhere in the acl list, so it is not blacklisted
        Assert.assertTrue(canAccessKey(5, 1));
        Assert.assertTrue(canAccessKey(5, 2));
        Assert.assertFalse(canAccessKey(5, 3));
        Assert.assertTrue(canAccessKey(5, 4));
        Assert.assertTrue(canAccessKey(5, 5));
        Assert.assertTrue(canAccessKey(5, 6));

        // site id 6 is blacklisted by site 2
        Assert.assertTrue(canAccessKey(6, 1));
        Assert.assertFalse(canAccessKey(6, 2));
        Assert.assertFalse(canAccessKey(6, 3));
        Assert.assertTrue(canAccessKey(6, 4));
        Assert.assertTrue(canAccessKey(6, 5));
        Assert.assertTrue(canAccessKey(6, 6));
    }
}
