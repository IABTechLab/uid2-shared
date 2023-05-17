package com.uid2.shared.store;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.StoreScope;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class ScopedStoreReader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScopedStoreReader.class);
    private final DownloadCloudStorage metadataStreamProvider;
    private final StoreScope scope;
    private final Parser<T> parser;
    private final String dataTypeName;
    private final DownloadCloudStorage contentStreamProvider;
    private final AtomicReference<T> latestSnapshot = new AtomicReference<>();

    public ScopedStoreReader(DownloadCloudStorage fileStreamProvider, StoreScope scope, Parser<T> parser, String dataTypeName) {
        this.metadataStreamProvider = fileStreamProvider;
        this.scope = scope;
        this.parser = parser;
        this.dataTypeName = dataTypeName;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
    }

    public CloudPath getMetadataPath() {
        return scope.getMetadataPath();
    }

    public T getSnapshot() {
        return latestSnapshot.get();
    }

    public JsonObject getMetadata() throws Exception {
        String cloudPath = getMetadataPath().toString();
        try (InputStream stream = this.metadataStreamProvider.download(cloudPath)) {
            return Utils.toJsonObject(stream);
        }
    }

    private long loadContent(String path) throws Exception {
        try (InputStream inputStream = this.contentStreamProvider.download(path)) {
            final ParsingResult<T> parsed = parser.deserialize(inputStream);
            this.latestSnapshot.set(parsed.getData());
            LOGGER.info(String.format("Loaded %d %s", parsed.getCount(), dataTypeName));
            return parsed.getCount();
        }
    }

    public long loadContent(JsonObject metadata, String dataType) throws Exception {
        if (metadata == null) {
            throw new IllegalArgumentException(String.format("No metadata provided for loading data type %s, can not load content", dataType));
        }
        final JsonObject clientKeysMetadata = metadata.getJsonObject(dataType);
        final String path = clientKeysMetadata.getString("location");
        return loadContent(path);
    }
}
