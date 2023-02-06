package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;

public interface IKeysAclSnapshot {
    boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key);

    /**
     * This function when called with MissingAclMode.ALLOW_ALL will always return true if the key does not have an ACL
     * When called with MissingAclMode.DENY_ALL will always return false if there is no ACL
     * Client can always access their own keys
     *
     * @param clientKey the client key that is trying to access a key
     * @param key the key the client is trying to access
     * @param accessMethod the access mode described in the description
     *
     * @return Bool if client can access the given key
     */
    boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key, MissingAclMode accessMethod);
}

