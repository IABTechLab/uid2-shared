package com.uid2.shared.cloud;

import java.util.Map;

public interface TaggableCloudStorage extends ICloudStorage {

    void setTags(String cloudPath, Map<String, String> tags) throws CloudStorageException;
}
