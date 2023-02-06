package com.uid2.shared.store.ACLMode;

public enum MissingAclMode {
    /**
     * This mode means a key with no acl can be accessed by anyone
     */
    ALLOW_ALL,
    /**
     * This mode means a key with no acl cannot be access by anyone
     */
    DENY_ALL
}
