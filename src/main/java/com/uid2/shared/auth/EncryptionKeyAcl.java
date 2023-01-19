package com.uid2.shared.auth;

import com.uid2.shared.model.EncryptionKey;

import java.util.Objects;
import java.util.Set;

public class EncryptionKeyAcl {
    private final boolean isWhitelist;
    private final Set<Integer> accessList;

    public EncryptionKeyAcl(boolean isWhitelist, Set<Integer> accessList) {
        this.isWhitelist = isWhitelist;
        this.accessList = accessList;
    }

    public boolean canBeAccessedBySite(int siteId) {
        return isWhitelist == accessList.contains(siteId);
    }

    public boolean getIsWhitelist() { return isWhitelist; }
    public Set<Integer> getAccessList() { return accessList; }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof EncryptionKeyAcl)) return false;

        EncryptionKeyAcl b = (EncryptionKeyAcl) o;

        return this.isWhitelist == b.isWhitelist
                && this.accessList.equals(b.accessList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isWhitelist, accessList);
    }
}
