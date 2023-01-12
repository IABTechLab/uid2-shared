package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.AclSnapshot;
import com.uid2.shared.auth.EncryptionKeyAcl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class KeyAclParser implements Parser<AclSnapshot> {
    @Override
    public ParsingResult<AclSnapshot> deserialize(InputStream inputStream) throws IOException {
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

        return new ParsingResult<>(new AclSnapshot(aclMap), aclsSpec.size());
    }
}
