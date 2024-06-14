package com.uid2.shared.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S3Key s3Key = (S3Key) o;
        return id == s3Key.id &&
                siteId == s3Key.siteId &&
                activates == s3Key.activates &&
                created == s3Key.created &&
                Objects.equals(secret, s3Key.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteId, activates, created, secret);
    }
}
