package com.uid2.shared.cloud;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

public interface ICloudStorage extends DownloadCloudStorage {
    void upload(String localPath, String cloudPath) throws CloudStorageException;

    void upload(InputStream input, String cloudPath) throws CloudStorageException;

    void delete(String cloudPath) throws CloudStorageException;

    void delete(Collection<String> cloudPaths) throws CloudStorageException;

    List<String> list(String prefix) throws CloudStorageException;

    URL preSignUrl(String cloudPath) throws CloudStorageException;

    void setPreSignedUrlExpiry(long expiry);

    String mask(String cloudPath);
}

