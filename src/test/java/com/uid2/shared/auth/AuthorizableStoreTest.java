package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHashResult;
import com.uid2.shared.secret.KeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizableStoreTest {
    private static final KeyHasher KEY_HASHER = new KeyHasher();
    private static final Instant NOW = Instant.now();
    private static final String SITE_11_CLIENT_KEY = "UID2-C-L-11-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_12_CLIENT_KEY_1 = "UID2-C-L-12-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_12_CLIENT_KEY_2 = "UID2-C-L-12-ghijkl.ghijklghijklghijklghijklghijklghijklgh";
    private static final String SITE_13_CLIENT_KEY = "UID2-C-L-13-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_14_CLIENT_KEY = "UID2-C-L-14-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";

    private AuthorizableStore<ClientKey> clientKeyStore;

    @BeforeEach
    public void setUp() {
        List<ClientKey> clients = List.of(
                createClientKey(KEY_HASHER.hashKey(SITE_11_CLIENT_KEY), "client11", 11),
                createClientKey(KEY_HASHER.hashKey(SITE_12_CLIENT_KEY_1), "client12_1", 12),
                createClientKey(KEY_HASHER.hashKey(SITE_12_CLIENT_KEY_2), "client12_2", 12),
                createClientKey(KEY_HASHER.hashKey(SITE_13_CLIENT_KEY), "client13", 13)
        );

        this.clientKeyStore = new AuthorizableStore<>();
        this.clientKeyStore.refresh(clients);
    }

    @Test
    public void getAuthorizableByKey_returnsClientKey_withValidKey() {
        ClientKey client = clientKeyStore.getAuthorizableByKey(SITE_13_CLIENT_KEY);

        assertEquals("client13", client.getName());
    }

    @Test
    public void getAuthorizableByKey_returnsClientKey_withMultipleKeysUnderSameSiteId() {
        ClientKey client = clientKeyStore.getAuthorizableByKey(SITE_12_CLIENT_KEY_1);

        assertEquals("client12_1", client.getName());
    }

    @Test
    public void getAuthorizableByKey_returnsNull_withUnknownKeyAndValidSiteId() {
        ClientKey client = clientKeyStore.getAuthorizableByKey("UID2-C-L-11-abcdef.ghijklghijklghijklghijklghijklghijklgh");

        assertNull(client);
    }

    @Test
    public void getAuthorizableByKey_returnsNull_withUnknownKeyAndUnknownSiteId() {
        ClientKey client = clientKeyStore.getAuthorizableByKey("UID2-C-L-15-abcdef.ghijklghijklghijklghijklghijklghijklgh");

        assertNull(client);
    }

    @Test
    public void getAuthorizableByKey_returnsNull_withInvalidKeyFormat() {
        ClientKey client = clientKeyStore.getAuthorizableByKey("invalid-key");

        assertNull(client);
    }

    @Test
    public void getAuthorizableByHash_returnsClientKey_withValidKeyHash() {
        String client1KeyHash = clientKeyStore.getAuthorizableByKey(SITE_11_CLIENT_KEY).getKeyHash();
        ClientKey client = clientKeyStore.getAuthorizableByHash(client1KeyHash);

        assertEquals("client11", client.getName());
    }

    @Test
    public void getAuthorizableByHash_returnsNull_withInvalidBase64KeyHash() {
        ClientKey client = clientKeyStore.getAuthorizableByHash("invalid-key-hash");

        assertNull(client);
    }

    @Test
    public void getAuthorizableByHash_returnsNull_withUnknownKeyHash() {
        ClientKey client = clientKeyStore.getAuthorizableByHash("unknown");

        assertNull(client);
    }

    @Test
    public void refresh_returnsNewClients_afterRefresh() {
        ClientKey client4 = createClientKey(KEY_HASHER.hashKey(SITE_14_CLIENT_KEY), "client14", 14);
        clientKeyStore.refresh(List.of(client4));

        assertAll(
                "refresh returns new clients after refresh",
                () -> assertEquals("client14", clientKeyStore.getAuthorizableByKey(SITE_14_CLIENT_KEY).getName()),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_11_CLIENT_KEY))
        );
    }

    private ClientKey createClientKey(KeyHashResult khr, String name, int siteId) {
        return new ClientKey("", khr.getHash(), khr.getSalt(), "", name, NOW, Set.of(), siteId);
    }
}
