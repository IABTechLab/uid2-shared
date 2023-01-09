package com.uid2.shared.cloud;

import com.uid2.shared.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryStorageMock implements ICloudStorage {

    public Map<String, byte[]> localFileSystemMock = new HashMap<>();
    public Map<String, byte[]> cloudFileSystemMock = new HashMap<>();

    public void save(byte[] content, String fullPath) {
        localFileSystemMock.put(fullPath, content);
    }

    public byte[] load(String fullPath) {
        return localFileSystemMock.get(fullPath);
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        cloudFileSystemMock.put(cloudPath, localFileSystemMock.get(localPath));
    }

    @Override
    public void upload(InputStream input, String cloudPath) throws CloudStorageException {
        try {
            cloudFileSystemMock.put(cloudPath, Utils.readToEndAsString(input).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CloudStorageException("cannot read all contents as string from inputstream", e);
        }
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        byte[] data = cloudFileSystemMock.get(cloudPath);
        if (data == null) {
            String error = "Trying to download missing path `" + cloudPath + "`. Available keys: " + cloudFileSystemMock.keySet();
            throw new CloudStorageException(error);
        }

        byte[] content = data.clone();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        return inputStream;
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        cloudFileSystemMock.remove(cloudPath);
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        for (String p : cloudPaths) {
            delete(p);
        }
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        return cloudFileSystemMock.keySet()
                .stream()
                .filter(x -> x.startsWith(prefix))
                .collect(Collectors.toList());
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        try {
            return new URL("mock://" + cloudPath);
        } catch (Exception e) {
            throw new CloudStorageException("cannot generate url", e);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {

    }

    @Override
    public String mask(String cloudPath) {
        return cloudPath;
    }
}