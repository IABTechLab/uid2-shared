package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.model.EncryptionKey;
import com.uid2.shared.store.ACLMode.MissingAclMode;

public interface IKeysAclSnapshot {
    /**
     * This function determines if a clientkey can access an encryption key
     * This is equivalent to calling canClientAccessKey with MissingAclMode.Allow_All. I.E. this function will always
     *   return a client can access a key with no ACL
     * @param clientKey the client key that is trying to access a key
     * @param key the key the client is trying to access
     * @return bool if client can access the given key
     */
    boolean canClientAccessKey(ClientKey clientKey, EncryptionKey key);

    /**
     * This function determines if a clientkey can access an encryption key
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

