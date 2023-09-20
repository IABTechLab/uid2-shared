package com.uid2.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.store.CloudPath;
import com.uid2.shared.store.reader.IMetadataVersionedStore;
import com.uid2.shared.store.IOperatorKeyProvider;
import com.uid2.shared.store.scope.StoreScope;
import com.uid2.shared.utils.ObjectMapperFactory;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RotatingOperatorKeyProvider implements IOperatorKeyProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingOperatorKeyProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.build();

    private final DownloadCloudStorage metadataStreamProvider;
    private final DownloadCloudStorage contentStreamProvider;
    private final StoreScope scope;
    private final AuthorizableStore<OperatorKey> operatorKeyStore;


    public RotatingOperatorKeyProvider(DownloadCloudStorage metadataStreamProvider, DownloadCloudStorage contentStreamProvider, StoreScope scope) {
        this.metadataStreamProvider = metadataStreamProvider;
        this.contentStreamProvider = contentStreamProvider;
        this.scope = scope;
        this.operatorKeyStore = new AuthorizableStore<>(OperatorKey.class);
    }

    public CloudPath getMetadataPath() {
        return scope.getMetadataPath();
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        String cloudPath = getMetadataPath().toString();
        try (InputStream s = this.metadataStreamProvider.download(cloudPath)) {
            return Utils.toJsonObject(s);
        }
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonObject operatorsMetadata = metadata.getJsonObject("operators");
        final String contentPath = operatorsMetadata.getString("location");
        try (InputStream contentStream = this.contentStreamProvider.download(contentPath)) {
            return loadOperators(contentStream);
        }
    }

    private long loadOperators(InputStream contentStream) throws Exception {
        String operatorKeysJson = CharStreams.toString(new InputStreamReader(contentStream, Charsets.UTF_8));
        List<OperatorKey> operatorKeys = Arrays.asList(OBJECT_MAPPER.readValue(operatorKeysJson, OperatorKey[].class));
        operatorKeyStore.refresh(operatorKeys);
        LOGGER.info("Loaded " + operatorKeys.size() + " operator profiles");
        return operatorKeys.size();
    }

    @Override
    public OperatorKey getOperatorKey(String token) {
        return operatorKeyStore.getAuthorizableByKey(token);
    }

    @Override
    public OperatorKey getOperatorKeyFromHash(String hash) {
        return (OperatorKey) this.operatorKeyStore.getAuthorizableByHash(hash);
    }

    @Override
    public Collection<OperatorKey> getAll() {
        return operatorKeyStore.getAuthorizables();
    }

    @Override
    public IAuthorizable get(String key) {
        return getOperatorKey(key);
    }
}
