package com.uid2.shared.auth;

import com.uid2.shared.cloud.EmbeddedResourceStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.reader.RotatingClientKeyProvider;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RotatingClientKeyProviderTest {
    @Test
    public void testGetOldestClientKeyBySiteId() throws Exception {
        RotatingClientKeyProvider provider = new RotatingClientKeyProvider(
                new EmbeddedResourceStorage(RotatingClientKeyProviderTest.class),
                new GlobalScope(new CloudPath("/com.uid2.shared/test/clients/metadata.json")));

        JsonObject metadata = provider.getMetadata();
        provider.loadContent(metadata);
        assertEquals(1, provider.getVersion(metadata));

        // a site that has multiple client keys
        ClientKey expected = provider.getClientKey("trusted-partner-key");
        assertNotNull(expected);
        ClientKey actual = provider.getOldestClientKey(expected.getSiteId());
        assertNotNull(actual);
        assertThat(actual).isEqualTo(expected);

        // a site that doesn't have client keys
        actual = provider.getOldestClientKey(1234567890);
        assertNull(actual);
    }
}
