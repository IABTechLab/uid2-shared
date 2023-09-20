package com.uid2.shared.store;

import com.uid2.shared.auth.Role;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.Service;
import com.uid2.shared.store.reader.RotatingServiceStore;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class RotatingServiceStoreTest {

    private AutoCloseable mocks;
    @Mock
    ICloudStorage cloudStorage;
    private RotatingServiceStore serviceStore;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        serviceStore = new RotatingServiceStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @AfterEach
    public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject services = new JsonObject();
        services.put("location", location);
        metadata.put("services", services);
        return metadata;
    }

    private Service addService(JsonArray content, int serviceId, int siteId, String name, Set<Role> roles) {
        Service service = new Service(serviceId, siteId, name, roles);
        JsonObject jo = JsonObject.mapFrom(service);
        content.add(jo);
        return service;
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = serviceStore.loadContent(makeMetadata("locationPath"));
        assertEquals(0, count);
        assertEquals(0, serviceStore.getAllServices().size());
    }

    @Test
    public void loadContentMultipleServices() throws Exception {
        JsonArray content = new JsonArray();
        Service s1 = addService(content, 1, 123, "Test Service 1", Set.of());
        Service s2 = addService(content, 2, 123, "test1", Set.of(Role.GENERATOR));
        Service s3 = addService(content, 3, 124, "Test Service 1", Set.of(Role.GENERATOR, Role.SHARING_PORTAL));
        Service s4 = addService(content, 4, 125, "test2", Set.of(Role.CLIENTKEY_ISSUER));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceStore.loadContent(makeMetadata("locationPath"));
        assertEquals(4, count);

        assertEquals(s1, serviceStore.getService(1));
        assertEquals(s2, serviceStore.getService(2));
        assertEquals(s3, serviceStore.getService(3));
        assertEquals(s4, serviceStore.getService(4));

        assertTrue(serviceStore.getAllServices().containsAll(Arrays.asList(s1, s2, s3, s4)));
    }
}
