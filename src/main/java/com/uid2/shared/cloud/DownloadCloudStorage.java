package com.uid2.shared.cloud;

import java.io.InputStream;

public interface DownloadCloudStorage {
    InputStream download(String cloudPath) throws CloudStorageException;
}
