package com.uid2.shared.auth;

import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.IKeysAclSnapshot;

import java.util.Map;

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