package com.uid2.shared.model;

public class S3Key {
    private int id;
    private int siteId;
    private long activates;
    private long created;
    private String secret;

    public S3Key() {}

    public S3Key(int id, int siteId, long activates, long created, String secret) {
        this.id = id;
        this.siteId = siteId;
        this.activates = activates;
        this.created = created;
        this.secret = secret;
    }

    // Getter and Setter methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public long getActivates() {
        return activates;
    }

    public void setActivates(long activates) {
        this.activates = activates;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
