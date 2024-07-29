package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.S3Key;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RotatingS3KeyProviderTest {

    @Mock
    private DownloadCloudStorage fileStreamProvider;

    @Mock
    private StoreScope scope;

    @Mock
    private ScopedStoreReader<Map<Integer, S3Key>> reader;

    private RotatingS3KeyProvider rotatingS3KeyProvider;

    private static final long CURRENT_TIME = 1688000000000L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rotatingS3KeyProvider = new RotatingS3KeyProvider(fileStreamProvider, scope);
        // Inject the mock reader into the RotatingS3KeyProvider
        rotatingS3KeyProvider.reader = reader;
        rotatingS3KeyProvider.siteToKeysMap = new HashMap<>();
    }

    @Test
    void testGetMetadata() throws Exception {
        JsonObject expectedMetadata = new JsonObject().put("version", 1L);
        when(reader.getMetadata()).thenReturn(expectedMetadata);

        JsonObject metadata = rotatingS3KeyProvider.getMetadata();
        assertEquals(expectedMetadata, metadata);
    }

    @Test
    void testGetMetadataPath() {
        CloudPath expectedPath = new CloudPath("some/path");
        when(reader.getMetadataPath()).thenReturn(expectedPath);

        CloudPath path = rotatingS3KeyProvider.getMetadataPath();
        assertEquals(expectedPath, path);
    }

    @Test
    void testGetVersion() {
        JsonObject metadata = new JsonObject().put("version", 1L);
        long version = rotatingS3KeyProvider.getVersion(metadata);
        assertEquals(1L, version);
    }

    @Test
    void testLoadContentWithMetadata() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.loadContent(metadata, "s3encryption_keys")).thenReturn(1L);

        long version = rotatingS3KeyProvider.loadContent(metadata);
        assertEquals(1L, version);
    }

    @Test
    void testLoadContent() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "s3encryption_keys")).thenReturn(1L);

        rotatingS3KeyProvider.loadContent();
        verify(reader, times(1)).getMetadata();
        verify(reader, times(1)).loadContent(metadata, "s3encryption_keys");
    }

    @Test
    void testLoadContentEmptyArray() throws Exception {
        JsonObject metadata = new JsonObject().put("keys", new JsonArray());
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "s3encryption_keys")).thenReturn(0L);

        long version = rotatingS3KeyProvider.loadContent(metadata);
        assertEquals(0L, version);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertTrue(keys.isEmpty());
    }

    @Test
    void testGetAll() {
        Map<Integer, S3Key> expectedKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        expectedKeys.put(1, key1);
        expectedKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(expectedKeys);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertEquals(expectedKeys, keys);
    }

    @Test
    void testGetAllEmpty() {
        when(reader.getSnapshot()).thenReturn(new HashMap<>());

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertTrue(keys.isEmpty());
    }

    @Test
    void testGetAllNullSnapshot() {
        when(reader.getSnapshot()).thenReturn(null);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    void testUpdateExistingKey() throws Exception {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key existingKey = new S3Key(1, 123, 1687635529, 1687808329, "oldSecret");
        existingKeys.put(1, existingKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        S3Key updatedKey = new S3Key(1, 123, 1687635529, 1687808329, "newSecret");
        existingKeys.put(1, updatedKey);

        rotatingS3KeyProvider.getAll().put(1, updatedKey);

        verify(reader, times(1)).getSnapshot();
        assertEquals("newSecret", rotatingS3KeyProvider.getAll().get(1).getSecret());
    }

    @Test
    void testAddNewKey() throws Exception {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);

        S3Key newKey = new S3Key(2, 456, 1687808429, 1687808329, "newSecret");
        existingKeys.put(2, newKey);

        rotatingS3KeyProvider.getAll().put(2, newKey);

        verify(reader, times(1)).getSnapshot();
        assertEquals("newSecret", rotatingS3KeyProvider.getAll().get(2).getSecret());
    }

    @Test
    void testHandleNonExistentKey() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key existingKey = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, existingKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        S3Key nonExistentKey = rotatingS3KeyProvider.getAll().get(99);
        assertNull(nonExistentKey);
    }

    @Test
    void testGetKeysForSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        S3Key key3 = new S3Key(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<S3Key> retrievedKeys = rotatingS3KeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(2, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(key1));
        assertTrue(retrievedKeys.contains(key2));

        Collection<S3Key> noKeysRetrieved = rotatingS3KeyProvider.getKeysForSite(789);
        assertNotNull(noKeysRetrieved);
        assertTrue(noKeysRetrieved.isEmpty());
    }

    @Test
    void testGetAllWithSingleKey() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key singleKey = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertEquals(1, keys.size());
        assertEquals(singleKey, keys.get(1));
    }

    @Test
    void testGetEncryptionKeyForSiteWithSingleKey() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key singleKey = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        S3Key retrievedKey = rotatingS3KeyProvider.getEncryptionKeyForSite(123,CURRENT_TIME);
        assertNotNull(retrievedKey);
        assertEquals(singleKey, retrievedKey);
    }

    @Test
    void testGetKeysForSiteWithSingleKey() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key singleKey = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, singleKey);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<S3Key> retrievedKeys = rotatingS3KeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(1, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(singleKey));
    }

    @Test
    void testGetKeysForSiteWithMultipleKeys() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        Collection<S3Key> retrievedKeys = rotatingS3KeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(2, retrievedKeys.size());
        assertTrue(retrievedKeys.contains(key1));
        assertTrue(retrievedKeys.contains(key2));
    }

    @Test
    void testGetEncryptionKeyForNonExistentSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        existingKeys.put(1, key1);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        assertThrows(IllegalStateException.class, () -> rotatingS3KeyProvider.getEncryptionKeyForSite(456, CURRENT_TIME));
    }

    @Test
    void testGetAllWhenReaderThrowsException() {
        when(reader.getSnapshot()).thenThrow(new RuntimeException("Reader exception"));

        assertThrows(RuntimeException.class, () -> rotatingS3KeyProvider.getAll());
    }

    @Test
    void testLoadContentWhenReaderThrowsException() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "s3encryption_keys")).thenThrow(new RuntimeException("Load content exception"));

        assertThrows(RuntimeException.class, () -> rotatingS3KeyProvider.loadContent());
    }

    @Test
    void testGetEncryptionKeyForSiteWithEmptyMap() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);
        assertThrows(IllegalStateException.class, () -> rotatingS3KeyProvider.getEncryptionKeyForSite(123, CURRENT_TIME));
    }

    @Test
    void testGetKeysForSiteWithEmptyMap() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);
        Collection<S3Key> retrievedKeys = rotatingS3KeyProvider.getKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(0, retrievedKeys.size());
    }

    @Test
    void testGetEncryptionKeyForSiteWithMultipleKeysAndNonExistentSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);
        assertThrows(IllegalStateException.class, () -> rotatingS3KeyProvider.getEncryptionKeyForSite(456, CURRENT_TIME));
    }

    @Test
    void testGetKeysForSiteWithMultipleKeysAndNonExistentSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);
        Collection<S3Key> retrievedKeys = rotatingS3KeyProvider.getKeysForSite(456);
        assertNotNull(retrievedKeys);
        assertEquals(0, retrievedKeys.size());
    }

    @Test
    void testUpdateSiteToKeysMapping() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        S3Key key3 = new S3Key(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        // Verify the behavior that depends on siteToKeysMap
        Collection<S3Key> site123Keys = rotatingS3KeyProvider.getKeysForSite(123);
        Collection<S3Key> site456Keys = rotatingS3KeyProvider.getKeysForSite(456);

        assertEquals(2, site123Keys.size());
        assertTrue(site123Keys.contains(key1));
        assertTrue(site123Keys.contains(key2));

        assertEquals(1, site456Keys.size());
        assertTrue(site456Keys.contains(key3));

        assertEquals(2, rotatingS3KeyProvider.getTotalSites());
        Set<Integer> allSiteIds = rotatingS3KeyProvider.getAllSiteIds();
        assertEquals(2, allSiteIds.size());
        assertTrue(allSiteIds.contains(123));
        assertTrue(allSiteIds.contains(456));
    }

    @Test
    void testLoadContentOverride() throws Exception {
        JsonObject metadata = new JsonObject();
        when(reader.getMetadata()).thenReturn(metadata);
        when(reader.loadContent(metadata, "s3encryption_keys")).thenReturn(1L);

        rotatingS3KeyProvider.loadContent();

        verify(reader, times(1)).getMetadata();
        verify(reader, times(1)).loadContent(metadata, "s3encryption_keys");
    }

    @Test
    void testGetAllSiteIds() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 456, 1687808429, 1687808329, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        Set<Integer> allSiteIds = rotatingS3KeyProvider.getAllSiteIds();
        assertEquals(2, allSiteIds.size());
        assertTrue(allSiteIds.contains(123));
        assertTrue(allSiteIds.contains(456));
    }

    @Test
    void testGetTotalSites() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687808329, "S3keySecretByteHere2");
        S3Key key3 = new S3Key(3, 456, 1687635529, 1687808329, "S3keySecretByteHere3");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        int totalSites = rotatingS3KeyProvider.getTotalSites();
        assertEquals(2, totalSites);
    }

    @Test
    void testGetKeysForSiteFromMap() {
        S3Key key1 = new S3Key(1, 100, 1687635529, 1687808329, "secret1");
        S3Key key2 = new S3Key(2, 100, 1687808429, 1687981229, "secret2");
        S3Key key3 = new S3Key(3, 200, 1687981329, 1688154129, "secret3");

        Map<Integer, List<S3Key>> testMap = new HashMap<>();
        testMap.put(100, Arrays.asList(key1, key2));
        testMap.put(200, Collections.singletonList(key3));

        rotatingS3KeyProvider.siteToKeysMap = testMap;

        List<S3Key> result1 = rotatingS3KeyProvider.getKeys(100);
        assertEquals(2, result1.size());
        assertTrue(result1.contains(key1));
        assertTrue(result1.contains(key2));

        List<S3Key> result2 = rotatingS3KeyProvider.getKeys(200);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(key3));

        List<S3Key> result3 = rotatingS3KeyProvider.getKeys(300);
        assertTrue(result3.isEmpty());
    }

    @Test
    void testGetKeysForSiteFromMapWithEmptyMap() {
        rotatingS3KeyProvider.siteToKeysMap = new HashMap<>();

        List<S3Key> result = rotatingS3KeyProvider.getKeys(100);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetKeysForSiteFromMapWithNullMap() {
        rotatingS3KeyProvider.siteToKeysMap = null;

        assertThrows(NullPointerException.class, () -> rotatingS3KeyProvider.getKeys(100));
    }

    @Test
    void testGetMostRecentKeysForSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687981229, "S3keySecretByteHere2");
        S3Key key3 = new S3Key(3, 123, 1687981329, 1688154129, "S3keySecretByteHere3");
        S3Key key4 = new S3Key(4, 123, 1688154229, 1688327029, "S3keySecretByteHere4");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        existingKeys.put(4, key4);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        List<S3Key> retrievedKeys = rotatingS3KeyProvider.getMostRecentKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(3, retrievedKeys.size());
        assertEquals(key4, retrievedKeys.get(0));
        assertEquals(key3, retrievedKeys.get(1));
        assertEquals(key2, retrievedKeys.get(2));
    }

    @Test
    void testGetMostRecentKeysForSiteWithLessThanThreeKeys() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, 1687635529, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, 1687808429, 1687981229, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        List<S3Key> retrievedKeys = rotatingS3KeyProvider.getMostRecentKeysForSite(123);
        assertNotNull(retrievedKeys);
        assertEquals(2, retrievedKeys.size());
        assertEquals(key2, retrievedKeys.get(0));
        assertEquals(key1, retrievedKeys.get(1));
    }

    @Test
    void testGetMostRecentKeysForSiteWithNoKeys() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(existingKeys);

        rotatingS3KeyProvider.updateSiteToKeysMapping();

        assertThrows(IllegalStateException.class, () -> rotatingS3KeyProvider.getMostRecentKeysForSite(123));
    }

    @Test
    void testGetEncryptionKeyForSite() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, CURRENT_TIME - 3000, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, CURRENT_TIME - 2000, 1687981229, "S3keySecretByteHere2");
        S3Key key3 = new S3Key(3, 123, CURRENT_TIME - 1000, 1688154129, "S3keySecretByteHere3");
        S3Key key4 = new S3Key(4, 123, CURRENT_TIME + 1000, 1688327029, "S3keySecretByteHere4"); // Future key
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        existingKeys.put(3, key3);
        existingKeys.put(4, key4);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        S3Key retrievedKey = rotatingS3KeyProvider.getEncryptionKeyForSite(123, CURRENT_TIME);
        assertNotNull(retrievedKey);
        assertEquals(key3, retrievedKey); // Should return the most recent active key
    }

    @Test
    void testGetEncryptionKeyForSiteWithNoActiveKeys() {
        Map<Integer, S3Key> existingKeys = new HashMap<>();
        S3Key key1 = new S3Key(1, 123, CURRENT_TIME + 1000, 1687808329, "S3keySecretByteHere1");
        S3Key key2 = new S3Key(2, 123, CURRENT_TIME + 2000, 1687981229, "S3keySecretByteHere2");
        existingKeys.put(1, key1);
        existingKeys.put(2, key2);
        when(reader.getSnapshot()).thenReturn(existingKeys);

        assertThrows(IllegalStateException.class,
                () -> rotatingS3KeyProvider.getEncryptionKeyForSite(123, CURRENT_TIME));
    }

    @Test
    public void testRaceCondition() throws InterruptedException, ExecutionException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        Map<Integer, S3Key> keysMap = new HashMap<>();
        keysMap.put(1, new S3Key(1, 88, 1687635529, 1687808329, "secret1"));
        keysMap.put(2, new S3Key(2, 88, 1687635530, 1687808330, "secret2"));
        keysMap.put(3, new S3Key(3, 88, 1687635531, 1687808331, "secret3"));
        keysMap.put(4, new S3Key(4, 89, 1687635532, 1687808332, "secret4"));

        when(reader.getSnapshot()).thenReturn(keysMap);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                latch.await();
                if (ThreadLocalRandom.current().nextBoolean()) {
                    rotatingS3KeyProvider.updateSiteToKeysMapping();
                } else {
                    rotatingS3KeyProvider.getKeysForSite(88);
                }
                return null;
            });
        }

        // Start all tasks simultaneously
        for (Callable<Void> task : tasks) {
            executorService.submit(task);
        }
        latch.countDown();  // Allow all threads to proceed

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        List<S3Key> keysForSite88 = rotatingS3KeyProvider.getKeysForSite(88).stream()
                .filter(key -> key.getSiteId() == 88)
                .collect(Collectors.toList());
        assertEquals(3, keysForSite88.size());
    }
}
