package com.uid2.shared.vertx;

import com.uid2.shared.cloud.CloudStorageException;
import com.uid2.shared.cloud.ICloudStorage;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

public interface ICloudSync {
    String toCloudPath(String path);

    String toLocalPath(String path);

    boolean refresh(Instant now, ICloudStorage fsCloud, ICloudStorage fsLocal,
                    Consumer<Set<String>> handleDownloads, Consumer<Set<String>> handleDeletes)
        throws CloudStorageException;
}
