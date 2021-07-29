// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.store;

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
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

public class RotatingKeyStoreTest {
    private AutoCloseable mocks;
    @Mock private ICloudStorage cloudStorage;
    private RotatingKeyStore keyStore;

    @Before public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        keyStore = new RotatingKeyStore(cloudStorage, "metadata");
    }

    @After public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(int systemKeyId, String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keys = new JsonObject();
        keys.put("location", location);
        metadata.put("keys", keys);
        metadata.put("system_key_id", systemKeyId);
        return metadata;
    }

    private InputStream makeInputStream(JsonArray content) {
        return new ByteArrayInputStream(content.toString().getBytes(Charset.forName("UTF-8")));
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
        Assert.assertNotNull(key);
        Assert.assertEquals(keyId, key.getId());
        Assert.assertEquals(siteId, key.getSiteId());
        Assert.assertArrayEquals(secret.getBytes(), key.getKeyBytes());
    }

    private void checkActiveKeySetEquals(Integer... expectedKeyIds) {
        List<Integer> actualKeyIds = keyStore.getSnapshot().getActiveKeySet().stream().map((key) -> key.getId()).collect(Collectors.toList());
        Assert.assertArrayEquals(expectedKeyIds, actualKeyIds.toArray(new Integer[0]));
    }

    @Test public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyStore.loadContent(makeMetadata(1, "locationPath"));
        Assert.assertEquals(0, count);
        Assert.assertNull(keyStore.getSnapshot().getMasterKey());
        Assert.assertNull(keyStore.getSnapshot().getKey(1));
        Assert.assertNull(keyStore.getSnapshot().getActiveSiteKey(2, Instant.now()));
        Assert.assertTrue(keyStore.getSnapshot().getActiveKeySet().isEmpty());
    }

    @Test public void loadContentMultipleKeysArray() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, null, "system", now);
        addKey(content, 102, 202, "key102site202", now);
        addKey(content, 103, 203, "key103site203", now);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyStore.loadContent(makeMetadata(101, "locationPath"));
        Assert.assertEquals(3, count);
        checkKeyMatches(keyStore.getSnapshot().getMasterKey(), 101, -1, "system");
        Assert.assertNull(keyStore.getSnapshot().getKey(1));
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, -1, "system");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, 202, "key102site202");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, 203, "key103site203");
        Assert.assertNull(keyStore.getSnapshot().getActiveSiteKey(2, now));
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(202, now), 102, 202, "key102site202");
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(203, now), 103, 203, "key103site203");
        checkActiveKeySetEquals(101, 102, 103);
    }

    @Test public void loadContentMultipleKeysForSameSite() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, 200, "key101site200", now.minusSeconds(50), now.plusSeconds(30));
        addKey(content, 102, 200, "key102site200", now.plusSeconds(1), now.plusSeconds(50)); // not active yet
        addKey(content, 103, 200, "key103site200", now.minusSeconds(10), now.plusSeconds(20));
        addKey(content, 104, 200, "key104site200", now.minusSeconds(5), now.minusSeconds(1)); // expired
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyStore.loadContent(makeMetadata(101, "locationPath"));
        Assert.assertEquals(4, count);
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, 200, "key101site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, 200, "key102site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, 200, "key103site200");
        checkKeyMatches(keyStore.getSnapshot().getKey(104), 104, 200, "key104site200");
        checkKeyMatches(keyStore.getSnapshot().getActiveSiteKey(200, now), 103, 200, "key103site200");
        checkActiveKeySetEquals(101, 102, 103, 104);
    }

    @Test public void loadContentMultipleSystemKeys() throws Exception {
        Instant now = Instant.now();
        JsonArray content = new JsonArray();
        addKey(content, 101, null, "system1", now.plusSeconds(1));
        addKey(content, 102, null, "system2", now);
        addKey(content, 103, null, "system3", now.minusSeconds(1));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = keyStore.loadContent(makeMetadata(102, "locationPath"));
        Assert.assertEquals(3, count);
        checkKeyMatches(keyStore.getSnapshot().getMasterKey(), 102, -1, "system2");
        checkKeyMatches(keyStore.getSnapshot().getKey(101), 101, -1, "system1");
        checkKeyMatches(keyStore.getSnapshot().getKey(102), 102, -1, "system2");
        checkKeyMatches(keyStore.getSnapshot().getKey(103), 103, -1, "system3");
        checkActiveKeySetEquals(101, 102, 103);
    }
}
