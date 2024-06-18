package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class S3Key {
    private final int id;
    private final int siteId;
    private final long activates;
    private final long created;
    private final String secret;

    @JsonCreator
    public S3Key(
            @JsonProperty("id") int id,
            @JsonProperty("site_id") int siteId,
            @JsonProperty("activates") long activates,
            @JsonProperty("created") long created,
            @JsonProperty("secret") String secret) {
        this.id = id;
        this.siteId = siteId;
        this.activates = activates;
        this.created = created;
        this.secret = secret;
    }

    public int getId() {
        return id;
    }

    public int getSiteId() {
        return siteId;
    }

    public long getActivates() {
        return activates;
    }

    public long getCreated() {
        return created;
    }

    public String getSecret() {
        return secret;
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
