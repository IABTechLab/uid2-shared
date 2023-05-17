package com.uid2.shared.auth;

import com.uid2.shared.Utils;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.model.EnclaveIdentifier;
import com.uid2.shared.store.reader.IMetadataVersionedStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class EnclaveIdentifierProvider implements IEnclaveIdentifierProvider, IMetadataVersionedStore {

    public static final String ENCLAVES_METADATA_PATH = "enclaves_metadata_path";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnclaveIdentifierProvider.class);

    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private final AtomicReference<Set<EnclaveIdentifier>> snapshot;
    private final List<IOperatorChangeHandler> changeEventListeners = new ArrayList<>();

    public EnclaveIdentifierProvider(ICloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = this.contentStreamProvider = fileStreamProvider;
        this.metadataPath = metadataPath;
        this.snapshot = new AtomicReference<>(new HashSet<>());
    }

    @Override
    public void addListener(IOperatorChangeHandler handler) throws IllegalArgumentException {
        if(handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        if(!changeEventListeners.contains(handler)) {
            changeEventListeners.add(handler);
            handler.handle(snapshot.get());
        }
    }

    @Override
    public void removeListener(IOperatorChangeHandler handler) {
        if(changeEventListeners.contains(handler)) {
            changeEventListeners.remove(handler);
        }
    }

    @Override
    public JsonObject getMetadata() throws Exception {
        try (InputStream s = this.metadataStreamProvider.download(this.metadataPath)) {
            return Utils.toJsonObject(s);
        }
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        JsonObject root = metadata.getJsonObject("enclaves");
        String path = root.getString("location");
        JsonArray idList;
        try (InputStream in = this.contentStreamProvider.download(path)) {
             idList = Utils.toJsonArray(in);
        }
        Set<EnclaveIdentifier> newSet = new HashSet<>();
        for (int i = 0; i < idList.size(); i++) {
            JsonObject item = idList.getJsonObject(i);
            EnclaveIdentifier id = new EnclaveIdentifier(
                item.getString("name"),
                item.getString("protocol"),
                item.getString("identifier"),
                item.getLong("created"));
            newSet.add(id);
        }

        LOGGER.info("Loaded " + newSet.size() + " enclave profiles");

        snapshot.set(newSet);
        for(IOperatorChangeHandler handler : changeEventListeners) {
            handler.handle(newSet);
        }

        return newSet.size();
    }

    @Override
    public Collection<EnclaveIdentifier> getAll() {
        return snapshot.get();
    }

    public String getMetadataPath() {
        return metadataPath;
    }
}
