package com.uid2.shared.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uid2.shared.secret.KeyHasher;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
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
    private final Cache<String, String> keyToHashCache;

    public AuthorizableStore(Class<T> cls) {
        this.authorizables = new AtomicReference<>(new AuthorizableStoreSnapshot(new ArrayList<>()));
        this.keyToHashCache = createCache(cls);
    }

    public void refresh(Collection<T> authorizablesToRefresh) {
        authorizables.set(new AuthorizableStoreSnapshot(authorizablesToRefresh));
    }

    public T getAuthorizableByKey(String key) {
        AuthorizableStoreSnapshot latest = authorizables.get();

        String cachedHash = keyToHashCache.getIfPresent(key);
        if (cachedHash != null) {
            return cachedHash.isBlank() ? null : latest.getAuthorizableByHash(wrapHashToByteBuffer(cachedHash));
        }

        Integer siteId = getSiteIdFromKey(key);
        List<byte[]> salts = siteId == null ? latest.getSalts() : latest.getSaltsBySiteId(siteId);
        T authorizable = getAuthorizable(key, salts, latest);

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

    private T getAuthorizable(String key, List<byte[]> salts, AuthorizableStoreSnapshot snapshot) {
        for (byte[] salt : salts) {
            byte[] keyHash = KEY_HASHER.hashKey(key, salt);
            T authorizable = snapshot.getAuthorizableByHash(ByteBuffer.wrap(keyHash));
            if (authorizable != null) {
                return authorizable;
            }
        }
        return null;
    }

    private Cache<String, String> createCache(Class<T> cls) {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        CacheMetricsCollector cacheMetricsCollector = new CacheMetricsCollector().register();
        cacheMetricsCollector.addCache(cls.getName() + "-cache", cache);

        return cache;
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
        private final Map<Integer, List<byte[]>> siteIdToSaltsMap;
        private final List<byte[]> salts;

        public AuthorizableStoreSnapshot(Collection<T> authorizables) {
            this.hashToAuthorizableMap = authorizables.stream()
                    .collect(Collectors.toMap(
                            a -> wrapHashToByteBuffer(a.getKeyHash()),
                            a -> a
                    ));

            this.siteIdToSaltsMap = authorizables.stream()
                    .filter(a -> a.getSiteId() != null)
                    .collect(Collectors.groupingBy(
                            IAuthorizable::getSiteId,
                            Collectors.mapping(a -> convertBase64StringToBytes(a.getKeySalt()), Collectors.toList())
                    ));

            this.salts = authorizables.stream()
                    .map(a -> convertBase64StringToBytes(a.getKeySalt()))
                    .collect(Collectors.toList());
        }

        public T getAuthorizableByHash(ByteBuffer hashBytes) {
            return hashToAuthorizableMap.get(hashBytes);
        }

        public List<byte[]> getSaltsBySiteId(int siteId) {
            return siteIdToSaltsMap.getOrDefault(siteId, List.of());
        }

        public List<byte[]> getSalts() {
            return salts;
        }
    }
}
