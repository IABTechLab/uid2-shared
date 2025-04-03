package com.uid2.shared.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SaltEntry {
    private final long id;
    private final String hashedId;
    private final long lastUpdated;
    private final String salt;
    @Setter
    private String previousSalt;
    @Setter
    private Long refreshFrom;

    public SaltEntry(long id, String hashedId, long lastUpdated, String salt, String previousSalt, Long refreshFrom) {
        this.id = id;
        this.hashedId = hashedId;
        this.lastUpdated = lastUpdated;
        this.salt = salt;
        this.previousSalt = previousSalt;
        this.refreshFrom = refreshFrom;
    }

    public SaltEntry(long id, String hashedId, long lastUpdated, String salt) {
        this(id, hashedId, lastUpdated, salt, salt, 0L);
    }
}
