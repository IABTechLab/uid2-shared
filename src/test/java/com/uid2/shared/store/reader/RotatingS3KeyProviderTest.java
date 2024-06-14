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

import java.util.HashMap;
import java.util.Map;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rotatingS3KeyProvider = new RotatingS3KeyProvider(fileStreamProvider, scope);
        // Inject the mock reader into the RotatingS3KeyProvider
        rotatingS3KeyProvider.reader = reader;
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
}
