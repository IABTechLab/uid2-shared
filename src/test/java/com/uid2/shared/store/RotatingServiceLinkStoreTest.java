package com.uid2.shared.store;

import com.uid2.shared.auth.Role;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.ServiceLink;
import com.uid2.shared.store.reader.RotatingServiceLinkStore;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RotatingServiceLinkStoreTest {
    @Mock
    ICloudStorage cloudStorage;
    private RotatingServiceLinkStore serviceLinkStore;

    @BeforeEach
    public void setup() {
        serviceLinkStore = new RotatingServiceLinkStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject serviceLinks = new JsonObject();
        serviceLinks.put("location", location);
        metadata.put("service_links", serviceLinks);
        return metadata;
    }

    private ServiceLink addServiceLink(JsonArray content, String linkId, int serviceId, int siteId, String name, Set<Role> roles, boolean disabled) {
        ServiceLink link = new ServiceLink(linkId, serviceId, siteId, name, roles, disabled);
        JsonObject jo = JsonObject.mapFrom(link);
        content.add(jo);
        return link;
    }

    @Test
    public void allConstructors(){
        ServiceLink link = new ServiceLink("abc123", 1, 123, "Test Service 1", Set.of());
        assertEquals("abc123", link.getLinkId());
        assertEquals(1, link.getServiceId());
        assertEquals(123, link.getSiteId());
        assertEquals("Test Service 1", link.getName());
        assertEquals(Set.of(), link.getRoles());
        assertEquals(false, link.isDisabled());

        link = new ServiceLink("abc123", 1, 123, "Test Service 1", Set.of(), true);
        assertEquals("abc123", link.getLinkId());
        assertEquals(1, link.getServiceId());
        assertEquals(123, link.getSiteId());
        assertEquals("Test Service 1", link.getName());
        assertEquals(Set.of(), link.getRoles());
        assertEquals(true, link.isDisabled());
    }

    @Test
    public void loadContent_emptyArray_loadsZeroServiceLinks() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));

        assertEquals(0, count);
        assertEquals(0, serviceLinkStore.getAllServiceLinks().size());
    }

    @Test
    public void loadContent_multipleServiceLinksStored_loadsAllServiceLinks() throws Exception {
        JsonArray content = new JsonArray();
        ServiceLink l1 = addServiceLink(content, "abc123", 1, 123, "Test Service 1", Set.of(), false);
        ServiceLink l2 = addServiceLink(content, "abc123", 2, 123, "test1", Set.of(Role.MAPPER), true);
        ServiceLink l3 = addServiceLink(content, "ghi789", 1, 123, "Test Service 1", Set.of(Role.MAPPER, Role.SHARER), false);
        ServiceLink l4 = addServiceLink(content, "jkl1011", 3, 124, "test2", null, true);
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));

        assertEquals(4, count);
        assertTrue(serviceLinkStore.getAllServiceLinks().containsAll(Arrays.asList(l1, l2, l3, l4)));
    }

    @Test
    public void getServiceLink_multipleServiceLinksStored_findsCorrectServiceLink() throws Exception {
        JsonArray content = new JsonArray();
        ServiceLink l1 = addServiceLink(content, "abc123", 1, 123, "Test Service 1", Set.of(), false);
        ServiceLink l2 = addServiceLink(content, "abc123", 2, 123, "test1", Set.of(Role.MAPPER), true);
        ServiceLink l3 = addServiceLink(content, "ghi789", 1, 123, "Test Service 1", Set.of(Role.MAPPER, Role.SHARER), false);

        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));

        List<ServiceLink> expected = List.of(l1, l2, l3);
        List<ServiceLink> actual = List.of(
                serviceLinkStore.getServiceLink(1, "abc123"),
                serviceLinkStore.getServiceLink(2, "abc123"),
                serviceLinkStore.getServiceLink(1, "ghi789"));

        assertEquals(expected, actual);
        assertNull(serviceLinkStore.getServiceLink(4, "missing"));
    }

    @Test
    public void createService_nullRole_createsServiceLinkWithEmptySetOfRoles() throws Exception {
        JsonArray content = new JsonArray();
        ServiceLink sl = addServiceLink(content, "jkl1011", 3, 124, "Test Service", null, false);

        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = serviceLinkStore.loadContent(makeMetadata("locationPath"));

        assertEquals(1, count);
        assertEquals(serviceLinkStore.getServiceLink(3, "jkl1011"), new ServiceLink("jkl1011", 3, 124, "Test Service", Set.of()));
    }
}
