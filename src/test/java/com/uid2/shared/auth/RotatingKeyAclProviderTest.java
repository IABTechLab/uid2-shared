package com.uid2.shared.auth;

import com.uid2.shared.Utils;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.cloud.ICloudStorage;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

public class RotatingKeyAclProviderTest {
    private AutoCloseable mocks;
    @Mock private ICloudStorage cloudStorage;
    private RotatingKeyAclProvider keyAclProvider;

    @Before public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keyAclProvider = new RotatingKeyAclProvider(cloudStorage, "metadata");
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

    private InputStream makeInputStream(JsonArray content) {
        return new ByteArrayInputStream(content.toString().getBytes(Charset.forName("UTF-8")));
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
        for(int i = 0; i < listedSiteIds.length; ++i) {
            list.add(listedSiteIds[i]);
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

    @Test public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyAclProvider.loadContent(makeMetadata("locationPath"));
        Assert.assertEquals(0, count);
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
