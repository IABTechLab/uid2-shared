package com.uid2.shared.model;

public class SaltEntry {
    private final long id;
    private final String hashedId;
    private final long lastUpdated;
    private final String salt;

    public SaltEntry(long id, String hashedId, long lastUpdated, String salt) {
        this.id = id;
        this.lastUpdated = lastUpdated;
        this.hashedId = hashedId;
        this.salt = salt;
    }

    public long getId() {
        return id;
    }

    public String getHashedId() {
        return hashedId;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public String getSalt() {
        return salt;
    }
}
