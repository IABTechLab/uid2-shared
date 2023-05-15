package com.uid2.shared.cloud;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DryRunStorageMock implements TaggableCloudStorage {
    private static final List<String> EMPTY_LIST = new ArrayList<>();
    private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

    private boolean isVerbose;
    public DryRunStorageMock(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    @Override
    public void upload(String localPath, String cloudPath) throws CloudStorageException {
        logMessage("upload", "(" + localPath + ", " + cloudPath + ")");
        return;
    }

    @Override
    public void upload(InputStream inputStream, String cloudPath) throws CloudStorageException {
        logMessage("upload", "(stream, " + cloudPath + ")");
        return;
    }

    @Override
    public InputStream download(String cloudPath) throws CloudStorageException {
        logMessage("download", "(" + cloudPath + ")");
        return EMPTY_STREAM;
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        logMessage("delete", "(" + cloudPath + ")");
    }

    @Override
    public void delete(Collection<String> cloudPaths) throws CloudStorageException {
        logMessage("delete", "(..." + cloudPaths.size() + " paths...)");
        for (String p : cloudPaths) delete(p);
    }

    @Override
    public List<String> list(String prefix) throws CloudStorageException {
        logMessage("delete", "(" + prefix + ")");
        return EMPTY_LIST;
    }

    @Override
    public URL preSignUrl(String cloudPath) throws CloudStorageException {
        logMessage("preSignUrl", "(" + cloudPath + ")");
        try {
            return new URL("mock://" + cloudPath);
        } catch (Exception e) {
            throw new CloudStorageException("dryRun::preSignUrl error: " + e.getMessage(), e);
        }
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
        logMessage("setPreSignedUrlExpiry", "(" + expiry + ")");
    }

    @Override
    public String mask(String cloudPath) {
        logMessage("mask", "(" + cloudPath + ")");
        return cloudPath;
    }

    @Override
    public void setTags(String cloudPath, Map<String, String> tags) throws CloudStorageException {
        logMessage("setTags", "(..." + tags.size() + " tags...)");
        tags.forEach((k, v) -> logMessage("tag", String.format("key: %s, value: %s", k, v)));
    }

    private void logMessage(String method, String msg) {
        if (isVerbose) {
            System.out.println(method + " " + msg);
        }
    }
}
