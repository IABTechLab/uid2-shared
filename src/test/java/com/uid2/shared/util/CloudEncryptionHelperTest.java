package com.uid2.shared.util;

import com.uid2.shared.encryption.AesGcm;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.uid2.shared.util.CloudEncryptionHelpers.decryptInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudEncryptionHelperTest {
    private RotatingCloudEncryptionKeyProvider keyProvider;
    private CloudEncryptionKey encryptionKey;

    @BeforeEach
    void setUp() {
        keyProvider = mock(RotatingCloudEncryptionKeyProvider.class);

        // Generate a valid 32-byte AES key
        byte[] keyBytes = new byte[32];
        new Random().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        encryptionKey = new CloudEncryptionKey(1, 1, 0, 0, base64Key);

        Map<Integer, CloudEncryptionKey> mockKeyMap = new HashMap<>();
        mockKeyMap.put(encryptionKey.getId(), encryptionKey);
        when(keyProvider.getAll()).thenReturn(mockKeyMap);
        when(keyProvider.getKey(1)).thenReturn(mockKeyMap.get(1));
    }

    @Test
    void testDecryptionOfEncryptedContent() throws Exception {
        // Simulate encrypted content
        String secretKey = encryptionKey.getSecret();
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        byte[] encryptedPayload = AesGcm.encrypt("value1,value2".getBytes(StandardCharsets.UTF_8), secretKeyBytes);
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

        JsonObject encryptedJson = new JsonObject()
                .put("key_id", encryptionKey.getId())
                .put("encrypted_payload", encryptedPayloadBase64);

        String encryptedContent = encryptedJson.encodePrettily();

        InputStream encryptedInputStream = new ByteArrayInputStream(encryptedContent.getBytes(StandardCharsets.UTF_8));

        String decryptedContent = decryptInputStream(encryptedInputStream, keyProvider);

        assertThat(decryptedContent).isEqualTo("value1,value2");
    }
}
