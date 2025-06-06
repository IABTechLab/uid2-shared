package com.uid2.shared.store.reader;

import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RotatingCloudEncryptionKeyProviderTest {
    private static final long CURRENT_TIME = Instant.now().getEpochSecond();

    @Mock
    private ScopedStoreReader<Map<Integer, CloudEncryptionKey>> reader;
    private RotatingCloudEncryptionKeyProvider rotatingCloudEncryptionKeyProvider;

    @BeforeEach
    public void setup() {
        rotatingCloudEncryptionKeyProvider = new RotatingCloudEncryptionKeyProvider(reader);
    }

    @Test
    public void testGetMetadata() throws Exception {
        JsonObject expectedMetadata = new JsonObject().put("version", 1L);
        when(reader.getMetadata()).thenReturn(expectedMetadata);

        JsonObject metadata = rotatingCloudEncryptionKeyProvider.getMetadata();
        assertEquals(expectedMetadata, metadata);
    }

    @Test
    public void testGetMetadataPath() {
        CloudPath expectedPath = new CloudPath("some/path");
        when(reader.getMetadataPath()).thenReturn(expectedPath);

        CloudPath path = rotatingCloudEncryptionKeyProvider.getMetadataPath();
        assertEquals(expectedPath, path);
    }

    @Test
    public void testGetVersion() {
        JsonObject metadata = new JsonObject().put("version", 1L);
        long version = rotatingCloudEncryptionKeyProvider.getVersion(metadata);
        assertEquals(1L, version);
    }

    @Test
    public void testLoadContentWithMetadata() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.loadContent(metadata, "cloud_encryption_keys")).thenReturn(1L);

        long version = rotatingCloudEncryptionKeyProvider.loadContent(metadata);
        assertEquals(1L, version);
    }

    @Test
    public void testLoadContent() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "cloud_encryption_keys")).thenReturn(1L);

        rotatingCloudEncryptionKeyProvider.loadContent();
        verify(reader, times(1)).getMetadata();
        verify(reader, times(1)).loadContent(metadata, "cloud_encryption_keys");
    }

    @Test
    public void testLoadContentEmptyArray() throws Exception {
        JsonObject metadata = new JsonObject().put("keys", new JsonArray());
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "cloud_encryption_keys")).thenReturn(0L);

        long version = rotatingCloudEncryptionKeyProvider.loadContent(metadata);
        assertEquals(0L, version);

        Map<Integer, CloudEncryptionKey> keys = rotatingCloudEncryptionKeyProvider.getAll();
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetAll() {
        Map<Integer, CloudEncryptionKey> expectedKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        expectedKeys.put(1, key1);
        expectedKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(expectedKeys);

        Map<Integer, CloudEncryptionKey> keys = rotatingCloudEncryptionKeyProvider.getAll();
        assertEquals(expectedKeys, keys);
    }

    @Test
    public void testGetAllEmpty() {
        when(reader.getSnapshot()).thenReturn(new HashMap<>());

        Map<Integer, CloudEncryptionKey> keys = rotatingCloudEncryptionKeyProvider.getAll();
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetAllNullSnapshot() {
        when(reader.getSnapshot()).thenReturn(null);

        Map<Integer, CloudEncryptionKey> keys = rotatingCloudEncryptionKeyProvider.getAll();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGet() {
        Map<Integer, CloudEncryptionKey> expectedKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        expectedKeys.put(1, key1);
        expectedKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(expectedKeys);

        CloudEncryptionKey key = rotatingCloudEncryptionKeyProvider.getKey(1);
        assertEquals(key1, key);
    }

    @Test
    public void testGetEmpty() {
        when(reader.getSnapshot()).thenReturn(new HashMap<>());

        CloudEncryptionKey key = rotatingCloudEncryptionKeyProvider.getKey(1);
        assertNull(key);
    }

    @Test
    public void testGetNullSnapshot() {
        when(reader.getSnapshot()).thenReturn(null);

        CloudEncryptionKey key = rotatingCloudEncryptionKeyProvider.getKey(1);
        assertNull(key);
    }

    @Test
    public void testUpdateExistingKey() throws Exception {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey existingKey = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "oldSecret");
        existingKeys.put(1, existingKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        CloudEncryptionKey updatedKey = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "newSecret");
        existingKeys.put(1, updatedKey);

        rotatingCloudEncryptionKeyProvider.getAll().put(1, updatedKey);

        verify(reader, times(1)).getSnapshot();
        assertEquals("newSecret", rotatingCloudEncryptionKeyProvider.getAll().get(1).getSecret());
    }

    @Test
    public void testAddNewKey() throws Exception {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);

        CloudEncryptionKey newKey = new CloudEncryptionKey(2, 456, 1687808429, 1687808329, "newSecret");
        existingKeys.put(2, newKey);

        rotatingCloudEncryptionKeyProvider.getAll().put(2, newKey);

        verify(reader, times(1)).getSnapshot();
        assertEquals("newSecret", rotatingCloudEncryptionKeyProvider.getAll().get(2).getSecret());
    }

    @Test
    public void testHandleNonExistentKey() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey existingKey = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, existingKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        CloudEncryptionKey nonExistentKey = rotatingCloudEncryptionKeyProvider.getAll().get(99);
        assertNull(nonExistentKey);
    }

    @Test
    public void testGetKeysForSite() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        CloudEncryptionKey key3 = new CloudEncryptionKey(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<CloudEncryptionKey> retrievedKeys = rotatingCloudEncryptionKeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(2, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(key1));
        assertTrue(retrievedKeys.contains(key2));

        Collection<CloudEncryptionKey> noKeysRetrieved = rotatingCloudEncryptionKeyProvider.getKeysForSite(789);
        assertNotNull(noKeysRetrieved);
        assertTrue(noKeysRetrieved.isEmpty());
    }

    @Test
    public void testGetAllWithSingleKey() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey singleKey = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Map<Integer, CloudEncryptionKey> keys = rotatingCloudEncryptionKeyProvider.getAll();
        assertEquals(1, keys.size());
        assertEquals(singleKey, keys.get(1));
    }

    @Test
    public void testGetEncryptionKeyForSiteWithSingleKey() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey singleKey = new CloudEncryptionKey(1, 123, CURRENT_TIME - 1000, CURRENT_TIME + 1000, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        CloudEncryptionKey retrievedKey = rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(123);
        assertNotNull(retrievedKey);
        assertEquals(singleKey, retrievedKey);
    }

    @Test
    public void testGetKeysForSiteWithSingleKey() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey singleKey = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<CloudEncryptionKey> retrievedKeys = rotatingCloudEncryptionKeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(1, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(singleKey));
    }

    @Test
    public void testGetKeysForSiteWithMultipleKeys() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<CloudEncryptionKey> retrievedKeys = rotatingCloudEncryptionKeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(2, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(key1));
        assertTrue(retrievedKeys.contains(key2));
    }

    @Test
    public void testGetEncryptionKeyForNonExistentSite() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, CURRENT_TIME - 1000, CURRENT_TIME + 1000, "S3keySecretByteHere1");
        existingKeys.put(1, key1);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(456));

        assertEquals("No S3 keys available for encryption for site ID: 456", exception.getMessage());
    }

    @Test
    public void testGetAllWhenReaderThrowsException() {
        when(reader.getSnapshot()).thenThrow(new RuntimeException("Reader exception"));

        assertThrows(RuntimeException.class, () -> rotatingCloudEncryptionKeyProvider.getAll());
    }

    @Test
    public void testLoadContentWhenReaderThrowsException() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "cloud_encryption_keys")).thenThrow(new RuntimeException("Load content exception"));

        assertThrows(RuntimeException.class, () -> rotatingCloudEncryptionKeyProvider.loadContent());
    }

    @Test
    public void testGetEncryptionKeyForSiteWithEmptyMap() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);
        assertThrows(IllegalStateException.class, () -> rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(123));
    }

    @Test
    public void testGetKeysForSiteWithEmptyMap() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);
        Collection<CloudEncryptionKey> retrievedKeys = rotatingCloudEncryptionKeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(0, retrievedKeys.size());
    }

    @Test
    public void testGetEncryptionKeyForSiteWithMultipleKeysAndNonExistentSite() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, CURRENT_TIME - 2000, CURRENT_TIME + 1000, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, CURRENT_TIME - 1000, CURRENT_TIME + 2000, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(456));

        assertEquals("No S3 keys available for encryption for site ID: 456", exception.getMessage());
    }

    @Test
    public void testGetKeysForSiteWithMultipleKeysAndNonExistentSite() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);
        Collection<CloudEncryptionKey> retrievedKeys = rotatingCloudEncryptionKeyProvider.getKeysForSite(456);
        assertNotNull(retrievedKeys);
        assertEquals(0, retrievedKeys.size());
    }

    @Test
    public void testUpdateSiteToKeysMapping() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        CloudEncryptionKey key3 = new CloudEncryptionKey(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingCloudEncryptionKeyProvider.updateSiteToKeysMapping();

        // Verify the behavior that depends on siteToKeysMap
        Collection<CloudEncryptionKey> site123Keys = rotatingCloudEncryptionKeyProvider.getKeysForSite(123);
        Collection<CloudEncryptionKey> site456Keys = rotatingCloudEncryptionKeyProvider.getKeysForSite(456);

        assertEquals(2, site123Keys.size());
        assertTrue(site123Keys.contains(key1));
        assertTrue(site123Keys.contains(key2));

        assertEquals(1, site456Keys.size());
        assertTrue(site456Keys.contains(key3));

        assertEquals(2, rotatingCloudEncryptionKeyProvider.getTotalSites());
        Set<Integer> allSiteIds = rotatingCloudEncryptionKeyProvider.getAllSiteIds();
        assertEquals(2, allSiteIds.size());
        assertTrue(allSiteIds.contains(123));
        assertTrue(allSiteIds.contains(456));
    }

    @Test
    public void testLoadContentOverride() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "cloud_encryption_keys")).thenReturn(1L);

        rotatingCloudEncryptionKeyProvider.loadContent();

        verify(reader, times(1)).getMetadata();
        verify(reader, times(1)).loadContent(metadata, "cloud_encryption_keys");
    }

    @Test
    public void testGetAllSiteIds() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 456, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingCloudEncryptionKeyProvider.updateSiteToKeysMapping();

        Set<Integer> allSiteIds = rotatingCloudEncryptionKeyProvider.getAllSiteIds();
        assertEquals(2, allSiteIds.size());
        assertTrue(allSiteIds.contains(123));
        assertTrue(allSiteIds.contains(456));
    }

    @Test
    public void testGetTotalSites() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        CloudEncryptionKey key3 = new CloudEncryptionKey(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingCloudEncryptionKeyProvider.updateSiteToKeysMapping();

        int totalSites = rotatingCloudEncryptionKeyProvider.getTotalSites();
        assertEquals(2, totalSites);
    }

    @Test
    public void testGetKeysForSiteFromMap() {
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 100, 1687635529, 1687808329, "secret1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 100, 1687808429, 1687981229, "secret2");
        CloudEncryptionKey key3 = new CloudEncryptionKey(3, 200, 1687981329, 1688154129, "secret3");

        Map<Integer, List<CloudEncryptionKey>> testMap = new HashMap<>();
        testMap.put(100, Arrays.asList(key1, key2));
        testMap.put(200, Collections.singletonList(key3));

        rotatingCloudEncryptionKeyProvider.getSiteToKeysMap().clear();
        rotatingCloudEncryptionKeyProvider.getSiteToKeysMap().putAll(testMap);

        List<CloudEncryptionKey> result1 = rotatingCloudEncryptionKeyProvider.getKeys(100);
        assertEquals(2, result1.size());
        assertTrue(result1.contains(key1));
        assertTrue(result1.contains(key2));

        List<CloudEncryptionKey> result2 = rotatingCloudEncryptionKeyProvider.getKeys(200);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(key3));

        List<CloudEncryptionKey> result3 = rotatingCloudEncryptionKeyProvider.getKeys(300);
        assertTrue(result3.isEmpty());
    }

    @Test
    public void testGetKeysForSiteFromMapWithEmptyMap() {
        rotatingCloudEncryptionKeyProvider.getSiteToKeysMap().clear();

        List<CloudEncryptionKey> result = rotatingCloudEncryptionKeyProvider.getKeys(100);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEncryptionKeyForSite() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, CURRENT_TIME - 3000, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, CURRENT_TIME - 2000, 1687981229, "S3keySecretByteHere2");
        CloudEncryptionKey key3 = new CloudEncryptionKey(3, 123, CURRENT_TIME - 1000, 1688154129, "S3keySecretByteHere3");
        CloudEncryptionKey key4 = new CloudEncryptionKey(4, 123, CURRENT_TIME + 1000, 1688327029, "S3keySecretByteHere4"); // Future key
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        existingKeys.put(4, key4);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        CloudEncryptionKey retrievedKey = rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(123);
        assertNotNull(retrievedKey);
        assertEquals(key3, retrievedKey); // Should return the most recent active key
    }

    @Test
    public void testGetEncryptionKeyForSiteWithNoActiveKeys() {
        Map<Integer, CloudEncryptionKey> existingKeys = new HashMap<>();
        CloudEncryptionKey key1 = new CloudEncryptionKey(1, 123, CURRENT_TIME + 1000, 1687808329, "S3keySecretByteHere1");
        CloudEncryptionKey key2 = new CloudEncryptionKey(2, 123, CURRENT_TIME + 2000, 1687981229, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        assertThrows(IllegalStateException.class, () -> rotatingCloudEncryptionKeyProvider.getEncryptionKeyForSite(123));
    }

    private void test() {

    }
}
