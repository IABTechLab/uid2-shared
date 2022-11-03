package com.uid2.shared.cloud;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class PreSignedURLStorage extends URLStorageWithMetadata {
    private static final List<String> EMPTY_LIST = new ArrayList<>();

    public PreSignedURLStorage() {
    }

    public PreSignedURLStorage(Proxy proxy) {
        super(proxy);
    }

    @Override
    protected List<String> extractListFromMetadata() throws CloudStorageException {
        return EMPTY_LIST;
    }
}
