package com.uid2.shared.store;

import com.google.common.collect.ImmutableList;
import com.uid2.shared.cloud.InMemoryStorageMock;
import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import com.uid2.shared.store.scope.EncryptedScope;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.uid2.shared.TestUtilites.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptedScopedStoreReaderTest {
    private final CloudPath metadataPath = new CloudPath("test/test-metadata.json");
    private final CloudPath dataPath = new CloudPath("test/data.json");
    private final String dataType = "test-data-type";
    private final EncryptedScope scope = new EncryptedScope(metadataPath,123, false);
    private final JsonObject metadata = new JsonObject()
            .put(dataType, new JsonObject().put(
                    "location", dataPath.toString()
            ));
    private InMemoryStorageMock storage;
    private final TestDataParser parser = new TestDataParser();
    private RotatingCloudEncryptionKeyProvider keyProvider;
    private final int testSiteId = 123;
    private CloudEncryptionKey encryptionKey;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageMock();
        keyProvider = mock(RotatingCloudEncryptionKeyProvider.class);

        // Generate a valid 32-byte AES key
        byte[] keyBytes = new byte[32];
        new Random().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        encryptionKey = new CloudEncryptionKey(1, testSiteId, 0, 0, base64Key);

        Map<Integer, CloudEncryptionKey> mockKeyMap = new HashMap<>();
        mockKeyMap.put(encryptionKey.getId(), encryptionKey);
        when(keyProvider.getAll()).thenReturn(mockKeyMap);
        when(keyProvider.getKey(1)).thenReturn(mockKeyMap.get(1));
    }

    @Test
    void loadsAndDecryptsContentSuccessfully() throws Exception {
        // Simulate encrypted content
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8),  secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", encryptionKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), dataPath.toString());
        storage.upload(toInputStream(metadata.encodePrettily()), metadataPath.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, scope, parser, dataType, keyProvider);

        reader.loadContent(metadata, dataType);
        Collection<TestData> actual = reader.getSnapshot();

        Collection<TestData> expected = ImmutableList.of(
                new TestData("value1"),
                new TestData("value2")
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void raisesExceptionWhenNoDecryptionKeyFound() throws Exception {
        // Simulate encrypted content with an invalid key ID
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8),  secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", 999) // Invalid key ID
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), dataPath.toString());
        storage.upload(toInputStream(metadata.encodePrettily()), metadataPath.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, scope, parser, dataType, keyProvider);

        assertThatThrownBy(() -> reader.loadContent(metadata, dataType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No matching S3 key found for decryption");
    }

    @Test
    void testHandlingInvalidEncryptionKey() throws Exception {
        // Set key provider to return an empty map
        when(keyProvider.getAll()).thenReturn(new HashMap<>());
        when(keyProvider.getKey(anyInt())).thenReturn(null);

        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8), secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", encryptionKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), dataPath.toString());
        storage.upload(toInputStream(metadata.encodePrettily()), metadataPath.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, scope, parser, dataType, keyProvider);

        assertThatThrownBy(() -> reader.loadContent(metadata, dataType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No matching S3 key found for decryption");
    }

    @Test
    void testLoadWithMultipleEncryptionKeys() throws Exception {
        // Generate a new key and add it to the key provider
        byte[] newKeyBytes = new byte[32];
        new Random().nextBytes(newKeyBytes);
        String base64NewKey = Base64.getEncoder().encodeToString(newKeyBytes);
        CloudEncryptionKey newKey = new CloudEncryptionKey(2, testSiteId, 0, 0, base64NewKey);

        Map<Integer, CloudEncryptionKey> mockKeyMap = new HashMap<>();
        mockKeyMap.put(encryptionKey.getId(), encryptionKey);
        mockKeyMap.put(newKey.getId(), newKey);
        when(keyProvider.getAll()).thenReturn(mockKeyMap);
        when(keyProvider.getKey(2)).thenReturn(mockKeyMap.get(2));

        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8), newKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", newKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), dataPath.toString());
        storage.upload(toInputStream(metadata.encodePrettily()), metadataPath.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, scope, parser, dataType, keyProvider);

        reader.loadContent(metadata, dataType);
        Collection<TestData> actual = reader.getSnapshot();

        Collection<TestData> expected = ImmutableList.of(
                new TestData("value1"),
                new TestData("value2")
        );
        assertThat(actual).isEqualTo(expected);
    }

    private static class TestData {
        private final String field1;

        public TestData(String field1) {
            this.field1 = field1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return Objects.equals(field1, testData.field1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1);
        }
    }

    private static class TestDataParser implements Parser<Collection<TestData>> {
        @Override
        public ParsingResult<Collection<TestData>> deserialize(InputStream inputStream) throws IOException {
            List<TestData> result = Arrays.stream(readInputStream(inputStream)
                            .split(","))
                    .map(TestData::new)
                    .collect(Collectors.toList());
            return new ParsingResult<>(result, result.size());
        }

        private static String readInputStream(InputStream inputStream) {
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));
        }
    }
}

