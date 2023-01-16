package com.uid2.shared.model;

import com.uid2.shared.model.KeyIdentifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public final class EncryptionKey {
    private final int id;
    private final byte[] keyBytes;
    private final Instant created;
    private final Instant activates;
    private final Instant expires;
    private final int siteId;

    public EncryptionKey(int id, byte[] keyBytes, Instant created, Instant activates, Instant expires, int siteId) {
        this.id = id;
        this.keyBytes = keyBytes;
        this.created = created;
        this.expires = expires;
        this.activates = activates;
        this.siteId = siteId;
    }

    public int getId() {
        return id;
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getActivates() {
        return activates;
    }

    public Instant getExpires() {
        return expires;
    }

    public int getSiteId() { return siteId; }

    public KeyIdentifier getKeyIdentifier() {
        return new KeyIdentifier(this.id);
    }

    public boolean isExpired(Instant asOf) { return !expires.isAfter(asOf); }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof EncryptionKey)) return false;

        // typecast o to Complex so that we can compare data members
        EncryptionKey b = (EncryptionKey) o;

        // Compare the data members and return accordingly
        return this.id == b.id
                && Arrays.equals(this.keyBytes, b.keyBytes)
                && this.created.equals(b.created)
                && this.activates.equals(b.activates)
                && this.expires.equals(b.expires)
                && this.siteId == b.siteId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, keyBytes, created, activates, expires, siteId);
    }
}
