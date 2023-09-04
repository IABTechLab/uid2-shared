package com.uid2.shared.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uid2.shared.secret.KeyHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuthorizableStore<T extends IAuthorizable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizableStore.class);
    private static final KeyHasher KEY_HASHER = new KeyHasher();

    private final AtomicReference<AuthorizableStoreSnapshot> authorizables;
    private final Cache<String, String> keyToHashCache;
    private final Cache<Integer, String> siteIdToHashCache;

    public AuthorizableStore() {
        this.authorizables = new AtomicReference<>(new AuthorizableStoreSnapshot(new ArrayList<>()));
        this.keyToHashCache = createToHashCache(String.class, 10);
        this.siteIdToHashCache = createToHashCache(Integer.class, 10);
    }

    public void refresh(Collection<T> authorizablesToRefresh) {
        authorizables.set(new AuthorizableStoreSnapshot(authorizablesToRefresh));
    }

    public T getFromKey(String key) {
        AuthorizableStoreSnapshot latest = authorizables.get();
        Integer siteId = getSiteIdFromKey(key);

        T cachedAuthorizable = getFromCache(key, siteId);
        if (cachedAuthorizable != null) {
            return cachedAuthorizable;
        }

        if (siteId != null) {
            T authorizable = latest.getAuthorizableBySiteId(siteId);
            siteIdToHashCache.put(siteId, authorizable == null ? "" : authorizable.getKeyHash());
            return authorizable;
        } else {
            for (byte[] salt : latest.getSalts()) {
                byte[] keyHash = KEY_HASHER.hashKey(key, salt);
                T authorizable = latest.getAuthorizableByHash(ByteBuffer.wrap(keyHash));
                if (authorizable != null) {
                    keyToHashCache.put(key, authorizable.getKeyHash());
                    return authorizable;
                }
            }
            keyToHashCache.put(key, "");
            return null;
        }
    }

    public T getFromKeyHash(String keyHash) {
        AuthorizableStoreSnapshot latest = authorizables.get();

        byte[] keyHashBytes;
        try {
            keyHashBytes = Base64.getDecoder().decode(keyHash);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid base64 key hash: {}", keyHash);
            return null;
        }
        return latest.getAuthorizableByHash(ByteBuffer.wrap(keyHashBytes));
    }

    private <K> Cache<K, String> createToHashCache(Class<K> clazz, int expireMinutes) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(expireMinutes))
                .recordStats()
                .build();
    }

    private Integer getSiteIdFromKey(String key) {
        Pattern keyPattern = Pattern.compile("(?:UID2|EUID)-[CO]-[LTIP]-([0-9]+)-.{6}\\..{38}");
        Matcher matcher = keyPattern.matcher(key);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    private T getFromCache(String key, Integer siteId) {
        String hash = null;

        if (siteId != null && siteIdToHashCache.asMap().containsKey(siteId)) {
            hash = siteIdToHashCache.getIfPresent(siteId);
        }
        if (keyToHashCache.asMap().containsKey(key)) {
            hash = keyToHashCache.getIfPresent(key);
        }

        return StringUtils.isBlank(hash) ? null : getFromKeyHash(hash);
    }

    private class AuthorizableStoreSnapshot {
        private final Map<ByteBuffer, T> authorizablesKeyMap;
        private final Map<Integer, T> authorizablesSiteMap;
        private final Collection<byte[]> salts;

        public AuthorizableStoreSnapshot(Collection<T> authorizablesKeyMap) {
            this.authorizablesKeyMap = authorizablesKeyMap.stream()
                    .collect(Collectors.toMap(
                            a -> ByteBuffer.wrap(Base64.getDecoder().decode(a.getKeyHash())),
                            a -> a));
            this.authorizablesSiteMap = authorizablesKeyMap.stream()
                    .collect(Collectors.toMap(
                            IAuthorizable::getSiteId,
                            a -> a));
            this.salts = authorizablesKeyMap.stream()
                    .map(a -> Base64.getDecoder().decode(a.getKeySalt()))
                    .collect(Collectors.toList());
        }

        public T getAuthorizableByHash(ByteBuffer hashBytes) {
            return authorizablesKeyMap.get(hashBytes);
        }

        public T getAuthorizableBySiteId(int siteId) {
            return authorizablesSiteMap.get(siteId);
        }

        public Collection<byte[]> getSalts() {
            return salts;
        }
    }
}
