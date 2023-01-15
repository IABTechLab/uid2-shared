package com.uid2.shared.auth;

import com.uid2.shared.model.EncryptionKey;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof EncryptionKeyAcl)) return false;

        // typecast o to Complex so that we can compare data members
        EncryptionKeyAcl b = (EncryptionKeyAcl) o;

        // Compare the data members and return accordingly
        return this.isWhitelist == b.isWhitelist
                && this.accessList.equals(b.accessList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isWhitelist, accessList);
    }
}
