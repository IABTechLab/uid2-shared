package com.uid2.shared.auth;

import com.uid2.shared.secret.KeyHasher;

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
                    .filter(a -> a.getKeyHash() != null) // TODO: remove this filter when all keys have hashes
                    .collect(Collectors.toMap(
                            a -> ByteBuffer.wrap(Base64.getDecoder().decode(a.getKeyHash())),
                            a -> a
                    ));
            this.salts = authorizables.stream()
                    .filter(a -> a.getKeySalt() != null) // TODO: remove this filter when all keys have salts
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

    private final AtomicReference<AuthorizableStoreSnapshot> authorizables;
    private final KeyHasher keyHasher;

    public AuthorizableStore() {
        this.authorizables = new AtomicReference<>(new AuthorizableStoreSnapshot(new ArrayList<>()));
        this.keyHasher = new KeyHasher();
    }

    public void refresh(Collection<T> authorizables) {
        this.authorizables.set(new AuthorizableStoreSnapshot(authorizables));
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
            return null;
        }

        return latest.getAuthorizables().get(ByteBuffer.wrap(keyHashBytes));
    }
}
