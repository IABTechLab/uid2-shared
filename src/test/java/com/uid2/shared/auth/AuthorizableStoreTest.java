package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHashResult;
import com.uid2.shared.secret.KeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizableStoreTest {
    private static final KeyHasher KEY_HASHER = new KeyHasher();
    private static final Instant NOW = Instant.now();
    private static final String SITE_11_CLIENT_KEY = "UID2-C-L-11-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_12_CLIENT_KEY_1 = "UID2-C-L-12-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_12_CLIENT_KEY_2 = "UID2-C-L-12-ghijkl.ghijklghijklghijklghijklghijklghijklgh";
    private static final String SITE_13_CLIENT_KEY = "UID2-C-L-13-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
    private static final String SITE_13_CLIENT_KEY_LEGACY = "abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";

    private AuthorizableStore<ClientKey> clientKeyStore;

    @BeforeEach
    public void setup() {
        List<ClientKey> clients = List.of(
                createClientKey(KEY_HASHER.hashKey(SITE_11_CLIENT_KEY), "client11", 11),
                createClientKey(KEY_HASHER.hashKey(SITE_12_CLIENT_KEY_1), "client12_1", 12),
                createClientKey(KEY_HASHER.hashKey(SITE_12_CLIENT_KEY_2), "client12_2", 12),
                createClientKey(KEY_HASHER.hashKey(SITE_13_CLIENT_KEY), "client13", 13),
                createClientKey(KEY_HASHER.hashKey(SITE_13_CLIENT_KEY_LEGACY), "client13_legacy", 13)
        );

        this.clientKeyStore = new AuthorizableStore<>(ClientKey.class);
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
    public void getAuthorizableByKey_returnsClientKey_withLegacyFormat() {
        ClientKey client = clientKeyStore.getAuthorizableByKey(SITE_13_CLIENT_KEY_LEGACY);

        assertEquals("client13_legacy", client.getName());
    }

    @Test
    public void getAuthorizableByKey_returnsKey_withNoSiteId() {
        String key = "abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
        SitelessAuthorizable sitelessAuthorizable = new SitelessAuthorizable(KEY_HASHER.hashKey(key), "noSiteClient");
        AuthorizableStore<SitelessAuthorizable> sitelessAuthorizableStore = new AuthorizableStore<>(SitelessAuthorizable.class);
        sitelessAuthorizableStore.refresh(List.of(sitelessAuthorizable));

        assertEquals("noSiteClient", sitelessAuthorizableStore.getAuthorizableByKey(key).getContact());
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
    public void getAuthorizableByKey_returnsNull_withMissingSiteId() {
        ClientKey client = clientKeyStore.getAuthorizableByKey("UID2-C-L-abcdef.ghijklghijklghijklghijklghijklghijklgh");

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
        String key = "UID2-C-L-14-abcdef.abcdefabcdefabcdefabcdefabcdefabcdefab";
        ClientKey client = createClientKey(KEY_HASHER.hashKey(key), "client14", 14);
        clientKeyStore.refresh(List.of(client));

        assertAll(
                "refresh returns new clients after refresh",
                () -> assertEquals("client14", clientKeyStore.getAuthorizableByKey(key).getName()),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_11_CLIENT_KEY)),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_12_CLIENT_KEY_1)),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_12_CLIENT_KEY_2)),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_13_CLIENT_KEY)),
                () -> assertNull(clientKeyStore.getAuthorizableByKey(SITE_13_CLIENT_KEY_LEGACY))
        );
    }

    private ClientKey createClientKey(KeyHashResult khr, String name, int siteId) {
        return new ClientKey("", khr.getHash(), khr.getSalt(), "", name, NOW, Set.of(), siteId);
    }

    private static class SitelessAuthorizable implements IAuthorizable {
        private final String keyHash;
        private final String keySalt;
        private final String contact;

        public SitelessAuthorizable(KeyHashResult khr, String contact) {
            this.keyHash = khr.getHash();
            this.keySalt = khr.getSalt();
            this.contact = contact;
        }

        public String getKey() {
            return null;
        }

        @Override
        public String getKeyHash() {
            return keyHash;
        }

        @Override
        public String getKeySalt() {
            return keySalt;
        }

        @Override
        public String getContact() {
            return contact;
        }

        @Override
        public Integer getSiteId() {
            return null;
        }

        @Override
        public boolean isDisabled() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SitelessAuthorizable that = (SitelessAuthorizable) o;
            return Objects.equals(keyHash, that.keyHash)
                    && Objects.equals(keySalt, that.keySalt)
                    && Objects.equals(contact, that.contact);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyHash, keySalt, contact);
        }
    }
}
