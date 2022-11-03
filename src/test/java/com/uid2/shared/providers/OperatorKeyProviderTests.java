package com.uid2.shared.providers;

import com.uid2.shared.auth.OperatorKey;
import com.uid2.shared.auth.RotatingOperatorKeyProvider;
import com.uid2.shared.cloud.InMemoryStorageMock;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OperatorKeyProviderTests {
    @Test
    public void testLoadOperatorKeys() throws Exception {
        InMemoryStorageMock storage = new InMemoryStorageMock();
        String metadataString = "{\n" +
                "  \"version\": 2,\n" +
                "  \"generated\": 1620253519,\n" +
                "  \"operators\": {\n" +
                "    \"location\": \"cloud/operators/operators.json\"\n" +
                "  }\n" +
                "}";

        String contentString = "[\n" +
                "  {\n" +
                "    \"key\": \"test-partner-key\",\n" +
                "    \"name\": \"test-partner\",\n" +
                "    \"contact\": \"test-partner@uid2.com\",\n" +
                "    \"created\": 1617149276,\n" +
                "    \"protocol\": \"aws-nitro\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"trusted-partner-key\",\n" +
                "    \"name\": \"trusted-partner\",\n" +
                "    \"contact\": \"trusted-partner@uid2.com\",\n" +
                "    \"created\": 1617149276,\n" +
                "    \"protocol\": \"trusted\"\n" +
                "  }\n" +
                "]";

        storage.save(metadataString.getBytes(StandardCharsets.UTF_8), "local/operators/metadata.json");
        storage.save(contentString.getBytes(StandardCharsets.UTF_8), "local/operators/operators.json");
        storage.upload("local/operators/metadata.json", "cloud/operators/metadata.json");
        storage.upload("local/operators/operators.json", "cloud/operators/operators.json");

        RotatingOperatorKeyProvider provider = new RotatingOperatorKeyProvider(
                storage, storage, "cloud/operators/metadata.json");

        JsonObject metadata = provider.getMetadata();

        assertEquals(2, provider.getVersion(metadata));

        provider.loadContent(metadata);

        OperatorKey trusted = provider.getOperatorKey("trusted-partner-key");
        assertNotNull(trusted);
        assertEquals("trusted-partner-key", trusted.getKey());
        assertEquals("trusted-partner", trusted.getName());
        assertEquals("trusted-partner@uid2.com", trusted.getContact());
        assertEquals("trusted", trusted.getProtocol());
        assertEquals(1617149276, trusted.getCreated());

        OperatorKey test = provider.getOperatorKey("test-partner-key");
        assertNotNull(test);
        assertEquals("test-partner-key", test.getKey());
        assertEquals("test-partner", test.getName());
        assertEquals("test-partner@uid2.com", test.getContact());
        assertEquals("aws-nitro", test.getProtocol());
        assertEquals(1617149276, test.getCreated());

        assertEquals(2, provider.getAll().size());
    }
}
