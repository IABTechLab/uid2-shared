package com.uid2.shared.cloud;

import java.io.InputStream;
import java.util.Map;

public interface TaggableCloudStorage extends ICloudStorage {

    void setTags(String cloudPath, Map<String, String> tags) throws CloudStorageException;

    void upload(String localPath, String cloudPath, Map<String, String> tags) throws CloudStorageException;

    void upload(InputStream input, String cloudPath, Map<String, String> tags) throws CloudStorageException;

}
