package com.uid2.shared.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class KeysetKey {
    private final int id;
    private final byte[] keyBytes;
    private final Instant created;
    private final Instant activates;
    private final Instant expires;
    private final int keysetId;

    public KeysetKey(int id, byte[] keyBytes, Instant created, Instant activates, Instant expires, int keysetId) {
        this.id = id;
        this.keyBytes = keyBytes;
        this.created = created;
        this.expires = expires;
        this.activates = activates;
        this.keysetId = keysetId;
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

    public int getKeysetId() { return keysetId; }

    public KeyIdentifier getKeyIdentifier() {
        return new KeyIdentifier(this.id);
    }

    public boolean isExpired(Instant asOf) { return !expires.isAfter(asOf); }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof KeysetKey)) return false;

        KeysetKey b = (KeysetKey) o;

        return this.id == b.id
                && Arrays.equals(this.keyBytes, b.keyBytes)
                && this.created.equals(b.created)
                && this.activates.equals(b.activates)
                && this.expires.equals(b.expires)
                && this.keysetId == b.keysetId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, Arrays.hashCode(keyBytes), created, activates, expires, keysetId);
    }
}
