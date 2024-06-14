package com.uid2.shared.store.reader;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.S3Key;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.ScopedStoreReader;
import com.uid2.shared.store.scope.GlobalScope;
import com.uid2.shared.store.scope.StoreScope;
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
    void testGetAll() {
        Map<Integer, S3Key> expectedKeys = new HashMap<>();
        when(reader.getSnapshot()).thenReturn(expectedKeys);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertEquals(expectedKeys, keys);
    }

    @Test
    void testGetAllEmpty() {
        when(reader.getSnapshot()).thenReturn(null);

        Map<Integer, S3Key> keys = rotatingS3KeyProvider.getAll();
        assertTrue(keys.isEmpty());
    }
}
