package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHashResult;
import com.uid2.shared.secret.KeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
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

    private AuthorizableStore<ClientKey> store;

    @BeforeEach
    public void setUp() {
        List<ClientKey> clients = List.of(
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_1), "client1", 11),
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_2), "client2", 12),
                createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_3), "client3", 13)
        );

        this.store = new AuthorizableStore<>();
        this.store.refresh(clients);
    }

    @Test
    public void getFromKey_returnsClientKey_withValidKey() {
        ClientKey client = store.getFromKey(CLIENT_KEY_3);

        assertEquals("client3", client.getName());
    }

    @Test
    public void getFromKey_returnsNull_withUnknownKey() {
        ClientKey client = store.getFromKey("invalid-key");

        assertNull(client);
    }

    @Test
    public void getFromKeyHash_returnsClientKey_withValidKeyHash() {
        String client1KeyHash = store.getFromKey(CLIENT_KEY_1).getKeyHash();
        ClientKey client = store.getFromKeyHash(client1KeyHash);

        assertEquals("client1", client.getName());
    }

    @Test
    public void getFromKeyHash_returnsNull_withInvalidBase64KeyHash() {
        ClientKey client = store.getFromKeyHash("invalid-key-hash");

        assertNull(client);
    }

    @Test
    public void getFromKeyHash_returnsNull_withUnknownKeyHash() {
        ClientKey client = store.getFromKeyHash("invalidkeyhash");

        assertNull(client);
    }

    @Test
    public void refresh_returnsNewClients_afterRefresh() {
        ClientKey client4 = createClientKey(KEY_HASHER.hashKey(CLIENT_KEY_4), "client4", 14);
        store.refresh(List.of(client4));

        assertAll(
                "Refresh returns new clients after refresh",
                () -> assertEquals("client4", store.getFromKey(CLIENT_KEY_4).getName()),
                () -> assertNull(store.getFromKey(CLIENT_KEY_1))
        );
    }

    private ClientKey createClientKey(KeyHashResult khr, String name, int siteId) {
        return new ClientKey("", khr.getHash(), khr.getSalt(), "", name, NOW, Set.of(), siteId);
    }
}
