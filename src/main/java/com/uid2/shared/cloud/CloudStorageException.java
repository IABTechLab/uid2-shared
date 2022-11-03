package com.uid2.shared.cloud;

public class CloudStorageException extends Exception {
    public CloudStorageException(String msg) {
        super(msg);
    }

    public CloudStorageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
