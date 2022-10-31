package com.uid2.shared.cloud;

import com.uid2.shared.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MemCachedStorage implements ICloudStorage {
    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("MemCachedStorage::upload(localPath,cloudPath) method is not supported");
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            cache.put(cloudPath, Utils.streamToByteArray(input));
        } catch (IOException e) {
            throw new CloudStorageException("MemCachedStorage::upload error: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        return new ByteArrayInputStream(cache.get(cloudPath));
    }

    // more effective way to return the cache byte[] array directly
    public byte[] getBytes(String cloudPath) {
        return cache.get(cloudPath);
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        cache.remove(cloudPath);
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        for (String p : cloudPaths) delete(p);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        return Collections.list(cache.keys()).stream()
            .filter(f -> f.startsWith(prefix))
            .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("MemCachedStorage::preSignUrl method is not supported");
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }
}
