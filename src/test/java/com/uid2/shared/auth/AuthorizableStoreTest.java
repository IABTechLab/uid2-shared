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
    private static final String CLIENT_KEY_1 = "UID2-C-L-11-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String CLIENT_KEY_2 = "UID2-C-L-12-ghijkl.ghijklghijklghijklghijklghijklghijklgh";
    private static final String CLIENT_KEY_3 = "UID2-C-L-13-mnopqr.mnopqrmnopqrmnopqrmnopqrmnopqrmnopqrmn";
    private static final String CLIENT_KEY_4 = "UID2-C-L-14-stuvwx.stuvwxstuvwxstuvwxstuvwxstuvwxstuvwxst";

    private static final String ADMIN_KEY_1 = "UID2-A-L-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String ADMIN_KEY_2 = "UID2-A-L-ghijkl.ghijklghijklghijklghijklghijklghijklgh";
    private static final String ADMIN_KEY_3 = "UID2-A-L-mnopqr.mnopqrmnopqrmnopqrmnopqrmnopqrmnopqrmn";
    private static final String ADMIN_KEY_4 = "UID2-A-L-stuvwx.stuvwxstuvwxstuvwxstuvwxstuvwxstuvwxst";

    private AuthorizableStore<ClientKey> clientKeyStore;

    @BeforeEach
    public void setUp() {
        List<ClientKey> clients = List.of(
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_1), "client1", 11),
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_2), "client2", 12),
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_3), "client3", 13)
        );

        this.clientKeyStore = new AuthorizableStore<>();
        this.clientKeyStore.refresh(clients);
    }

    @Test
    public void getAuthorizableByKey_returnsClientKey_withValidKey() {
        ClientKey client = clientKeyStore.getAuthorizableByKey(CLIENT_KEY_3);

        assertEquals("client3", client.getName());
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
        String client1KeyHash = clientKeyStore.getAuthorizableByKey(CLIENT_KEY_1).getKeyHash();
        ClientKey client = clientKeyStore.getAuthorizableByHash(client1KeyHash);

        assertEquals("client1", client.getName());
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
        ClientKey client4 = createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_4), "client4", 14);
        clientKeyStore.refresh(List.of(client4));

        assertAll(
                "refresh returns new clients after refresh",
                () -> assertEquals("client4", clientKeyStore.getAuthorizableByKey(CLIENT_KEY_4).getName()),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(CLIENT_KEY_1))
        );
    }

    private ClientKey createClientKey(KeyHashResult khr, String name, int siteId) {
        return new ClientKey("", khr.getHash(), khr.getSalt(), "", name, NOW, Set.of(), siteId);
    }
}
