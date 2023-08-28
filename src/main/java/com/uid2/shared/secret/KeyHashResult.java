package com.uid2.shared.secret;

import java.util.Objects;

public class KeyHashResult {
    private final String hash;
    private final String salt;

    public KeyHashResult(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
    }

    public String getHash() {
        return hash;
    }

    public String getSalt() {
        return salt;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        // If the object is of a different type, return false
        if (!(o instanceof KeyHashResult)) return false;

        KeyHashResult b = (KeyHashResult) o;

        // Compare the data members and return accordingly
        return this.hash.equals(b.hash)
                && this.salt.equals(b.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, salt);
    }
}
