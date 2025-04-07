package com.uid2.shared.store;

import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.StoreScope;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import java.nio.charset.StandardCharsets;

import static com.uid2.shared.util.CloudEncryptionHelpers.decryptInputStream;

public class EncryptedScopedStoreReader<T> extends ScopedStoreReader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedScopedStoreReader.class);

    private final RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider;

    public EncryptedScopedStoreReader(DownloadCloudStorage fileStreamProvider, StoreScope scope, Parser<T> parser, String dataTypeName, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider) {
        super(fileStreamProvider, scope, parser, dataTypeName);
        this.cloudEncryptionKeyProvider = cloudEncryptionKeyProvider;
    }

    @Override
    protected long loadContent(String path) throws Exception {
        try (InputStream inputStream = this.contentStreamProvider.download(path)) {
            String decryptedContent = decryptInputStream(inputStream, cloudEncryptionKeyProvider, dataTypeName);
            ParsingResult<T> parsed = this.parser.deserialize(new ByteArrayInputStream(decryptedContent.getBytes(StandardCharsets.UTF_8)));
            latestSnapshot.set(parsed.getData());

            final int count = parsed.getCount();
            latestEntryCount.set(count);
            LOGGER.info("Loaded {} {}", count, dataTypeName);
            return count;
        } catch (Exception e) {
            LOGGER.error("Unable to load {}", dataTypeName);
            throw e;
        }
    }
}
