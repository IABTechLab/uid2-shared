package com.uid2.shared.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.uid2.shared.secret.KeyHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuthorizableStore<T extends IAuthorizable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizableStore.class);
    private static final Pattern KEY_PATTERN = Pattern.compile("(?:UID2|EUID)-[CO]-[LTIP]-([0-9]+)-.{6}\\..{38}");
    private static final KeyHasher KEY_HASHER = new KeyHasher();
    private static final int CACHE_MAX_SIZE = 100_000;

    private final AtomicReference<AuthorizableStoreSnapshot> authorizables;
    private final LoadingCache<String, String> keyToHashCache;

    public AuthorizableStore() {
        this.authorizables = new AtomicReference<>(new AuthorizableStoreSnapshot(new ArrayList<>()));
        this.keyToHashCache = createCache();
    }

    public void refresh(Collection<T> authorizablesToRefresh) {
        authorizables.set(new AuthorizableStoreSnapshot(authorizablesToRefresh));
    }

    public T getAuthorizableByKey(String key) {
        AuthorizableStoreSnapshot latest = authorizables.get();

        T authorizable;

        authorizable = getAuthorizableBySiteId(latest, key);
        if (authorizable != null) {
            return authorizable;
        }

        String cachedHash = keyToHashCache.get(key);
        if (cachedHash != null) {
            return cachedHash.isBlank() ? null : latest.getAuthorizableByHash(wrapHashToByteBuffer(cachedHash));
        }

        authorizable = getAuthorizableIfKeyExists(latest, key);
        keyToHashCache.put(key, authorizable == null ? "" : authorizable.getKeyHash());

        return authorizable;
    }

    public T getAuthorizableByHash(String hash) {
        ByteBuffer hashBytes = wrapHashToByteBuffer(hash);
        if (hashBytes == null) {
            return null;
        }

        AuthorizableStoreSnapshot latest = authorizables.get();
        return latest.getAuthorizableByHash(hashBytes);
    }

    private T getAuthorizableBySiteId(AuthorizableStoreSnapshot snapshot, String key) {
        Integer siteId = getSiteIdFromKey(key);

        if (siteId != null) {
            T authorizable = snapshot.getAuthorizableBySiteId(siteId);
            if (authorizable == null) {
                return null;
            }

            byte[] hash = KEY_HASHER.hashKey(key, convertBase64StringToBytes(authorizable.getKeySalt()));
            return snapshot.getAuthorizableByHash(ByteBuffer.wrap(hash));
        }

        return null;
    }

    private T getAuthorizableIfKeyExists(AuthorizableStoreSnapshot snapshot, String key) {
        for (byte[] salt : snapshot.getSalts()) {
            byte[] keyHash = KEY_HASHER.hashKey(key, salt);
            T authorizable = snapshot.getAuthorizableByHash(ByteBuffer.wrap(keyHash));
            if (authorizable != null) {
                return authorizable;
            }
        }
        return null;
    }

    private static LoadingCache<String, String> createCache() {
        return Caffeine.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .build(k -> k);
    }

    private static ByteBuffer wrapHashToByteBuffer(String hash) {
        byte[] hashBytes = convertBase64StringToBytes(hash);
        return hashBytes == null ? null : ByteBuffer.wrap(hashBytes);
    }

    private static byte[] convertBase64StringToBytes(String str) {
        try {
            return Base64.getDecoder().decode(str);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid base64 string: {}", str);
            return null;
        }
    }

    private static Integer getSiteIdFromKey(String key) {
        Matcher matcher = KEY_PATTERN.matcher(key);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return null;
        }
    }

    private class AuthorizableStoreSnapshot {
        private final Map<ByteBuffer, T> hashToAuthorizableMap;
        private final Map<Integer, T> siteIdToAuthorizableMap;
        private final Set<byte[]> salts;

        public AuthorizableStoreSnapshot(Collection<T> hashToAuthorizableMap) {
            this.hashToAuthorizableMap = hashToAuthorizableMap.stream()
                    .collect(Collectors.toMap(
                            a -> wrapHashToByteBuffer(a.getKeyHash()),
                            a -> a));
            this.siteIdToAuthorizableMap = hashToAuthorizableMap.stream()
                    .filter(a -> a.getSiteId() != null)
                    .collect(Collectors.toMap(
                            IAuthorizable::getSiteId,
                            a -> a));
            this.salts = hashToAuthorizableMap.stream()
                    .map(a -> convertBase64StringToBytes(a.getKeySalt()))
                    .collect(Collectors.toSet());
        }

        public T getAuthorizableByHash(ByteBuffer hashBytes) {
            return hashToAuthorizableMap.get(hashBytes);
        }

        public T getAuthorizableBySiteId(int siteId) {
            return siteIdToAuthorizableMap.get(siteId);
        }

        public Collection<byte[]> getSalts() {
            return salts;
        }
    }
}
