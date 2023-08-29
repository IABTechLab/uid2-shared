package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHashResult;
import com.uid2.shared.secret.KeyHasher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class AuthorizableStoreTest {
    private AuthorizableStore<ClientKey> store;

    @Before
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
        Assert.assertEquals("client3", client.getName());
    }

    @Test
    public void getFromKey_returnsNull_withInvalidKey() {
        ClientKey client = this.store.getFromKey("test-key-99");
        Assert.assertNull(client);
    }

    @Test
    public void getFromKeyHash_returnsClientKey_withValidKeyHash() {
        String client1KeyHash = this.store.getFromKey("test-key-1").getKeyHash();

        ClientKey client = this.store.getFromKeyHash(client1KeyHash);
        Assert.assertEquals("client1", client.getName());
    }
}
