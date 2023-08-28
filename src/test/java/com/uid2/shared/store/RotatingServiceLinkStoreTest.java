package com.uid2.shared.store;

import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.ServiceLink;
import com.uid2.shared.store.reader.RotatingServiceLinkStore;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class RotatingServiceLinkStoreTest {
    private AutoCloseable mocks;
    @Mock
    ICloudStorage cloudStorage;
    private RotatingServiceLinkStore serviceLinkStore;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        serviceLinkStore = new RotatingServiceLinkStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @AfterEach
    public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject serviceLinks = new JsonObject();
        serviceLinks.put("location", location);
        metadata.put("service_links", serviceLinks);
        return metadata;
    }

    private ServiceLink addServiceLink(JsonArray content, String linkId, int serviceId, int siteId, String name) {

        ServiceLink l = new ServiceLink(linkId, serviceId, siteId, name);


        JsonObject service = new JsonObject();
        service.put("link_id", linkId);
        service.put("service_id", serviceId);
        service.put("site_id", siteId);
        service.put("name", name);

        content.add(service);
        return l;
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));
        assertEquals(0, count);
        assertEquals(0, serviceLinkStore.getAllServiceLinks().size());
    }

    @Test
    public void loadContentMultipleServices() throws Exception {
        JsonArray content = new JsonArray();
        ServiceLink l1 = addServiceLink(content, "abc123", 1, 123, "AWS Venice");
        ServiceLink l2 = addServiceLink(content, "def456", 1, 123, "test1");
        ServiceLink l3 = addServiceLink(content, "ghi789", 2, 124, "AWS Venice");
        ServiceLink l4 = addServiceLink(content, "jkl1011", 3, 125, "test2");
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));
        assertEquals(4, count);

        assertEquals(l1, serviceLinkStore.getServiceLink("abc123"));
        assertEquals(l2, serviceLinkStore.getServiceLink("def456"));
        assertEquals(l3, serviceLinkStore.getServiceLink("ghi789"));
        assertEquals(l4, serviceLinkStore.getServiceLink("jkl1011"));

        assertTrue(serviceLinkStore.getAllServiceLinks().contains(l1));
        assertTrue(serviceLinkStore.getAllServiceLinks().contains(l2));
        assertTrue(serviceLinkStore.getAllServiceLinks().contains(l3));
        assertTrue(serviceLinkStore.getAllServiceLinks().contains(l4));
    }
}
