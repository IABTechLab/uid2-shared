package com.uid2.shared.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SaltEntry {
    private final long id;
    private final String hashedId;
    private final long lastUpdated;
    private final String salt;
    @Setter private long refreshFrom;

    public SaltEntry(long id, String hashedId, long lastUpdated, String salt, long refreshFrom) {
        this.id = id;
        this.hashedId = hashedId;
        this.lastUpdated = lastUpdated;
        this.salt = salt;
        this.refreshFrom = refreshFrom;
    }

    public SaltEntry(long id, String hashedId, long lastUpdated, String salt) {
        this(id, hashedId, lastUpdated, salt, 0);
    }
}
