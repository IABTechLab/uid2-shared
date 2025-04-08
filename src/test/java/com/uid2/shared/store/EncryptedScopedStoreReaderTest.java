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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.uid2.shared.TestUtilites.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EncryptedScopedStoreReaderTest {
    private static final CloudPath METADATA_PATH = new CloudPath("test/test-metadata.json");
    private static final CloudPath DATA_PATH = new CloudPath("test/data.json");
    private static final String DATA_TYPE = "test-data-type";
    private static final int TEST_SITE_ID = 123;
    private static final EncryptedScope SCOPE = new EncryptedScope(METADATA_PATH, TEST_SITE_ID, false);
    private static final JsonObject METADATA = new JsonObject()
            .put(DATA_TYPE, new JsonObject().put(
                    "location", DATA_PATH.toString()
            ));
    private static final TestDataParser PARSER = new TestDataParser();

    @Mock
    private RotatingCloudEncryptionKeyProvider keyProvider;
    private InMemoryStorageMock storage;
    private CloudEncryptionKey encryptionKey;

    @BeforeEach
    public void setup() {
        storage = new InMemoryStorageMock();

        // Generate a valid 32-byte AES key
        byte[] keyBytes = new byte[32];
        new Random().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        encryptionKey = new CloudEncryptionKey(1, TEST_SITE_ID, 0, 0, base64Key);

        Map<Integer, CloudEncryptionKey> mockKeyMap = new HashMap<>();
        mockKeyMap.put(encryptionKey.getId(), encryptionKey);
        when(keyProvider.getAll()).thenReturn(mockKeyMap);
        when(keyProvider.getKey(1)).thenReturn(mockKeyMap.get(1));
    }

    @Test
    public void loadsAndDecryptsContentSuccessfully() throws Exception {
        // Simulate encrypted content
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8),  secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", encryptionKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), DATA_PATH.toString());
        storage.upload(toInputStream(METADATA.encodePrettily()), METADATA_PATH.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, SCOPE, PARSER, DATA_TYPE, keyProvider);

        reader.loadContent(METADATA, DATA_TYPE);
        Collection<TestData> actual = reader.getSnapshot();

        Collection<TestData> expected = ImmutableList.of(
                new TestData("value1"),
                new TestData("value2")
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void raisesExceptionWhenNoDecryptionKeyFound() throws Exception {
        // Simulate encrypted content with an invalid key ID
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8),  secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", 999) // Invalid key ID
                .put("encrypted_payload", encryptedPayloadBase64);

        storage.upload(toInputStream(encryptedJson.encodePrettily()), DATA_PATH.toString());
        storage.upload(toInputStream(METADATA.encodePrettily()), METADATA_PATH.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, SCOPE, PARSER, DATA_TYPE, keyProvider);

        assertThatThrownBy(() -> reader.loadContent(METADATA, DATA_TYPE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No matching key found for S3 file decryption");
    }

    @Test
    public void testHandlingInvalidEncryptionKey() throws Exception {
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

        storage.upload(toInputStream(encryptedJson.encodePrettily()), DATA_PATH.toString());
        storage.upload(toInputStream(METADATA.encodePrettily()), METADATA_PATH.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, SCOPE, PARSER, DATA_TYPE, keyProvider);

        assertThatThrownBy(() -> reader.loadContent(METADATA, DATA_TYPE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No matching key found for S3 file decryption");
    }

    @Test
    public void testLoadWithMultipleEncryptionKeys() throws Exception {
        // Generate a new key and add it to the key provider
        byte[] newKeyBytes = new byte[32];
        new Random().nextBytes(newKeyBytes);
        String base64NewKey = Base64.getEncoder().encodeToString(newKeyBytes);
        CloudEncryptionKey newKey = new CloudEncryptionKey(2, TEST_SITE_ID, 0, 0, base64NewKey);

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

        storage.upload(toInputStream(encryptedJson.encodePrettily()), DATA_PATH.toString());
        storage.upload(toInputStream(METADATA.encodePrettily()), METADATA_PATH.toString());

        EncryptedScopedStoreReader<Collection<TestData>> reader = new EncryptedScopedStoreReader<>(storage, SCOPE, PARSER, DATA_TYPE, keyProvider);

        reader.loadContent(METADATA, DATA_TYPE);
        Collection<TestData> actual = reader.getSnapshot();

        Collection<TestData> expected = ImmutableList.of(
                new TestData("value1"),
                new TestData("value2")
        );
        assertThat(actual).isEqualTo(expected);
    }

    private record TestData(String field1) {
    }

    private static class TestDataParser implements Parser<Collection<TestData>> {
        @Override
        public ParsingResult<Collection<TestData>> deserialize(InputStream inputStream) {
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
