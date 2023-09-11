package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHashResult;
import com.uid2.shared.secret.KeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class AuthorizableStoreTest {
    private AuthorizableStore<ClientKey> store;

    @BeforeEach
    public void setUp() {
        KeyHasher keyHasher = new KeyHasher();

        ArrayList<ClientKey> clients = new ArrayList<>();

        KeyHashResult clientHash1 = keyHasher.hashKey("test-key-1");
        clients.add(new ClientKey("", clientHash1.getHash(), clientHash1.getSalt(), "", "client1"));

        KeyHashResult clientHash2 = keyHasher.hashKey("test-key-2");
        clients.add(new ClientKey("", clientHash2.getHash(), clientHash2.getSalt(), "", "client2"));

        KeyHashResult clientHash3 = keyHasher.hashKey("test-key-3");
        clients.add(new ClientKey("", clientHash3.getHash(), clientHash3.getSalt(), "", "client3"));

        this.store = new AuthorizableStore<>();
        this.store.refresh(clients);
    }

    @Test
    public void getFromKey_returnsClientKey_withValidKey() {
        ClientKey client = this.store.getFromKey("test-key-3");
        assertEquals("client3", client.getName());
    }

    @Test
    public void getFromKey_returnsNull_withUnknownKey() {
        ClientKey client = this.store.getFromKey("invalid-key");
        assertNull(client);
    }

    @Test
    public void getFromKeyHash_returnsClientKey_withValidKeyHash() {
        String client1KeyHash = this.store.getFromKey("test-key-1").getKeyHash();

        ClientKey client = this.store.getFromKeyHash(client1KeyHash);
        assertEquals("client1", client.getName());
    }

    @Test
    public void getFromKeyHash_returnsNull_withInvalidBase64KeyHash() {
        ClientKey client = this.store.getFromKeyHash("invalid-key-hash");
        assertNull(client);
    }

    @Test
    public void getFromKeyHash_returnsNull_withUnknownKeyHash() {
        ClientKey client = this.store.getFromKeyHash("invalidkeyhash");
        assertNull(client);
    }

    @Test
    public void refresh_returnsNewClients_afterRefresh() {
        KeyHasher keyHasher = new KeyHasher();
        KeyHashResult clientHash1 = keyHasher.hashKey("test-key-4");
        ClientKey client4 = new ClientKey("", clientHash1.getHash(), clientHash1.getSalt(), "", "client4");

        this.store.refresh(List.of(client4));

        assertEquals("client4", this.store.getFromKey("test-key-4").getName());
        assertNull(this.store.getFromKey("test-key-1"));
    }
}
