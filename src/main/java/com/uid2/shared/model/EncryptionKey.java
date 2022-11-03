package com.uid2.shared.model;

import com.uid2.shared.model.KeyIdentifier;

import java.time.Instant;

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
}
