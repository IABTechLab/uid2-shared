package com.uid2.shared.store;

import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.StoreScope;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ScopedStoreReader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScopedStoreReader.class);

    private final DownloadCloudStorage metadataStreamProvider;
    private final StoreScope scope;
    private final Parser<T> parser;
    private final String dataTypeName;
    private final DownloadCloudStorage contentStreamProvider;
    private final AtomicReference<T> latestSnapshot;
    private final AtomicLong latestEntryCount = new AtomicLong(-1L);

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
        latestSnapshot = new AtomicReference<>();

        Gauge.builder("uid2_scoped_store_entry_count", latestEntryCount::get)
                .tag("store", dataTypeName)
                .description("gauge for " + dataTypeName + " store total entry count")
                .register(Metrics.globalRegistry);
    }

    public CloudPath getMetadataPath() {
        return scope.getMetadataPath();
    }

    public T getSnapshot() {
        return latestSnapshot.get();
    }

    public JsonObject getMetadata() throws Exception {
        String cloudPath = getMetadataPath().toString();
        try (InputStream stream = metadataStreamProvider.download(cloudPath)) {
            return Utils.toJsonObject(stream);
        }
    }

    private long loadContent(String path) throws Exception {
        try (InputStream inputStream = this.contentStreamProvider.download(path)) {
            ParsingResult<T> parsed = parser.deserialize(inputStream);
            latestSnapshot.set(parsed.getData());

            final int count = parsed.getCount();
            latestEntryCount.set(count);
            LOGGER.info(String.format("Loaded %d %s", count, dataTypeName));
            return count;
        }
        catch (Exception e) {
            // Do not log the message or the original exception as that may contain the pre-signed url
            LOGGER.error(String.format("Unable to load %s", dataTypeName));
            throw e;
        }
    }

    public long loadContent(JsonObject metadata, String dataType) throws Exception {
        if (metadata == null) {
            throw new IllegalArgumentException(String.format("No metadata provided for loading data type %s, can not load content", dataType));
        }

        JsonObject clientKeysMetadata = metadata.getJsonObject(dataType);
        String path = clientKeysMetadata.getString("location");
        return loadContent(path);
    }
}
