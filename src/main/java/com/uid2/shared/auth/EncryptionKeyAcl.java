package com.uid2.shared.auth;

import java.util.Set;

public class EncryptionKeyAcl {
    boolean isWhitelist;
    Set<Integer> accessList;

    public EncryptionKeyAcl(boolean isWhitelist, Set<Integer> accessList) {
        this.isWhitelist = isWhitelist;
        this.accessList = accessList;
    }

    public boolean canBeAccessedBySite(int siteId) {
        return isWhitelist == accessList.contains(siteId);
    }

    public boolean getIsWhitelist() { return isWhitelist; }
    public Set<Integer> getAccessList() { return accessList; }
}
