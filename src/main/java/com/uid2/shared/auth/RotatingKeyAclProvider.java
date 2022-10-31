package com.uid2.shared.auth;

import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.Utils;
import com.uid2.shared.attest.UidCoreClient;
import com.uid2.shared.cloud.ICloudStorage;
import com.uid2.shared.store.IKeyAclProvider;
import com.uid2.shared.store.IMetadataVersionedStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RotatingKeyAclProvider implements IKeyAclProvider, IMetadataVersionedStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatingKeyAclProvider.class);

    private final ICloudStorage metadataStreamProvider;
    private final ICloudStorage contentStreamProvider;
    private final String metadataPath;
    private final AtomicReference<AclSnapshot> latestSnapshot = new AtomicReference<AclSnapshot>();

    public RotatingKeyAclProvider(ICloudStorage fileStreamProvider, String metadataPath) {
        this.metadataStreamProvider = fileStreamProvider;
        if (fileStreamProvider instanceof UidCoreClient) {
            this.contentStreamProvider = ((UidCoreClient) fileStreamProvider).getContentStorage();
        } else {
            this.contentStreamProvider = fileStreamProvider;
        }
        this.metadataPath = metadataPath;
    }

    public String getMetadataPath() { return this.metadataPath; }

    @Override
    public JsonObject getMetadata() throws Exception {
        InputStream s = this.metadataStreamProvider.download(this.metadataPath);
        return Utils.toJsonObject(s);
    }

    @Override
    public long getVersion(JsonObject metadata) {
        return metadata.getLong("version");
    }

    @Override
    public long loadContent(JsonObject metadata) throws Exception {
        final JsonObject acls = metadata.getJsonObject("keys_acl");
        final String location = acls.getString("location");
        final InputStream inputStream = this.contentStreamProvider.download(location);
        final JsonArray aclsSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, EncryptionKeyAcl> aclMap = new HashMap<>();
        for(int i = 0; i < aclsSpec.size(); ++i) {
            final JsonObject aclSpec = aclsSpec.getJsonObject(i);
            final Integer siteId = aclSpec.getInteger("site_id");
            final JsonArray blacklistSpec = aclSpec.getJsonArray("blacklist");
            final JsonArray whitelistSpec = aclSpec.getJsonArray("whitelist");
            if(blacklistSpec == null && whitelistSpec == null) {
                continue;
            } else if (blacklistSpec != null && whitelistSpec != null) {
                throw new IllegalStateException(String.format("Site %d has both blacklist and whitelist specified, this is not allowed"));
            }
            final boolean isWhitelist = blacklistSpec == null;
            final JsonArray accessListSpec = isWhitelist ? whitelistSpec : blacklistSpec;
            final HashSet<Integer> accessList = new HashSet<>();
            for(int j = 0; j < accessListSpec.size(); ++j) {
                accessList.add(accessListSpec.getInteger(j));
            }

            aclMap.put(siteId, new EncryptionKeyAcl(isWhitelist, accessList));
        }
        this.latestSnapshot.set(new AclSnapshot(aclMap));
        LOGGER.info("Loaded " + aclsSpec.size() + " key acls");
        return aclsSpec.size();
    }

    public void loadContent() throws Exception {
        this.loadContent(this.getMetadata());
    }

    @Override
    public AclSnapshot getSnapshot(Instant asOf) {
        return this.latestSnapshot.get();
    }

    @Override
    public AclSnapshot getSnapshot() {
        return this.getSnapshot(Instant.now());
    }

    public class AclSnapshot implements IKeysAclSnapshot {
        private final Map<Integer, EncryptionKeyAcl> acls;

        public AclSnapshot(Map<Integer, EncryptionKeyAcl> acls) {
            this.acls = acls;
        }

        @Override
        public boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key) {
            // Client can always access their own keys
            if(clientKey.getSiteId() == key.getSiteId()) return true;

            EncryptionKeyAcl acl = acls.get(key.getSiteId());

            // No ACL: everyone has access to the site keys
            if(acl == null) return true;

            return acl.canBeAccessedBySite(clientKey.getSiteId());
        }

        public Map<Integer, EncryptionKeyAcl> getAllAcls() {
            return acls;
        }
    }
}
