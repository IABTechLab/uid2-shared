package com.uid2.shared.cloud;

import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.reader.RotatingClientKeyProvider;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class EmbeddedResourceStorageTest {
    @Test
    public void loadFromEmbeddedResourceStorage() throws Exception {
        RotatingClientKeyProvider fileProvider = new RotatingClientKeyProvider(
                new EmbeddedResourceStorage(EmbeddedResourceStorageTest.class),
                new GlobalScope(new CloudPath("/com.uid2.shared/test/clients/metadata.json")));

        JsonObject m = fileProvider.getMetadata();
        fileProvider.loadContent(m);
    }
}