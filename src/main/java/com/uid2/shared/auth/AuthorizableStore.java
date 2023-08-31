package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class AuthorizableStore<T extends IAuthorizable> {
    private class AuthorizableStoreSnapshot {
        private final Map<ByteBuffer, T> authorizables;
        private final Collection<byte[]> salts;

        public AuthorizableStoreSnapshot(Collection<T> authorizables) {
            this.authorizables = authorizables.stream()
                    .collect(Collectors.toMap(
                            a -> ByteBuffer.wrap(Base64.getDecoder().decode(a.getKeyHash())),
                            a -> a
                    ));
            this.salts = authorizables.stream()
                    .map(a -> Base64.getDecoder().decode(a.getKeySalt()))
                    .collect(Collectors.toList());
        }

        public Map<ByteBuffer, T> getAuthorizables() {
            return authorizables;
        }

        public Collection<byte[]> getSalts() {
            return salts;
        }
    }

    private static final KeyHasher keyHasher = new KeyHasher();
    private static final Logger logger = LoggerFactory.getLogger(AuthorizableStore.class);

    private final AtomicReference<AuthorizableStoreSnapshot> authorizables;

    public AuthorizableStore() {
        this.authorizables = new AtomicReference<>(new AuthorizableStoreSnapshot(new ArrayList<>()));
    }

    public void refresh(Collection<T> authorizablesToRefresh) {
        authorizables.set(new AuthorizableStoreSnapshot(authorizablesToRefresh));
    }

    public T getFromKey(String key) {
        AuthorizableStoreSnapshot latest = authorizables.get();
        // TODO: add caching
        for (byte[] salt : latest.getSalts()) {
            byte[] keyHash = keyHasher.hashKey(key, salt);
            T authorizable = latest.getAuthorizables().get(ByteBuffer.wrap(keyHash));
            if (authorizable != null) {
                return authorizable;
            }
        }
        return null;
    }

    public T getFromKeyHash(String keyHash) {
        AuthorizableStoreSnapshot latest = authorizables.get();

        byte[] keyHashBytes;
        try {
            keyHashBytes = Base64.getDecoder().decode(keyHash);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid base64 key hash: {}", keyHash);
            return null;
        }

        return latest.getAuthorizables().get(ByteBuffer.wrap(keyHashBytes));
    }
}
