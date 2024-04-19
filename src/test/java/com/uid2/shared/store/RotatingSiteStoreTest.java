package com.uid2.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.Site;
import com.uid2.shared.store.reader.RotatingSiteStore;
import com.uid2.shared.store.scope.GlobalScope;
import com.uid2.shared.util.Mapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class RotatingSiteStoreTest {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();
    private AutoCloseable mocks;
    @Mock
    ICloudStorage cloudStorage;
    private RotatingSiteStore siteStore;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        siteStore = new RotatingSiteStore(cloudStorage, new GlobalScope(new CloudPath("metadata")));
    }

    @AfterEach
    public void teardown() throws Exception {
        mocks.close();
    }

    private JsonObject makeMetadata(String location) {
        JsonObject metadata = new JsonObject();
        JsonObject keypairs = new JsonObject();
        keypairs.put("location", location);
        metadata.put("sites", keypairs);
        return metadata;
    }

    private Site addSite(JsonArray content, int siteId, String name, String description, boolean enabled, boolean visible, long created, Set<String> domains, Set<String> appNames) {

        Site s = new Site(siteId, name, description, enabled, new HashSet<>(), domains, appNames, visible, created);
        JsonNode jsonNode = OBJECT_MAPPER.convertValue(s, JsonNode.class);
        content.add(jsonNode);

        return s;
    }

    @Test
    public void loadContentEmptyArray() throws Exception {
        JsonArray content = new JsonArray();
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));
        final long count = siteStore.loadContent(makeMetadata("locationPath"));
        assertEquals(0, count);
        assertEquals(0, siteStore.getAllSites().size());
    }

    @Test
    public void loadContentMultipleSites() throws Exception {
        JsonArray content = new JsonArray();
        Site s1 = addSite(content, 123, "test-1", "test-1-desc", true, true, Instant.now().getEpochSecond(), new HashSet<>(), new HashSet<>());
        Site s2 = addSite(content, 124, "test-2", "test-2-desc", false, true, Instant.now().minusSeconds(100).getEpochSecond(), new HashSet<>(), new HashSet<>());
        Site s3 = addSite(content, 125, "test-3", "test-3-desc", true, false, Instant.now().plusSeconds(100).getEpochSecond(), new HashSet<>(), new HashSet<>());
        Site s4 = addSite(content, 126, "test-4", "test-4-desc", false, false, Instant.now().getEpochSecond(), new HashSet<>(List.of("testdomain1.com", "testdomain2.net")), new HashSet<>(List.of("testAppName1", "testAppName2")));
        when(cloudStorage.download("locationPath")).thenReturn(makeInputStream(content));

        final long count = siteStore.loadContent(makeMetadata("locationPath"));
        assertEquals(4, count);

        assertEquals(s1, siteStore.getSite(123));
        assertEquals(s2, siteStore.getSite(124));
        assertEquals(s3, siteStore.getSite(125));
        assertEquals(s4, siteStore.getSite(126));

        assertTrue(siteStore.getAllSites().contains(s1));
        assertTrue(siteStore.getAllSites().contains(s2));
        assertTrue(siteStore.getAllSites().contains(s3));
        assertTrue(siteStore.getAllSites().contains(s4));
    }
}
