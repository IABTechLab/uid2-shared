package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({ "id", "siteId", "activates", "created", "secret" })
public class CloudEncryptionKey {
    private final int id;
    private final int siteId;
    private final long activates;
    private final long created;
    private final String secret;

    @JsonCreator
    public CloudEncryptionKey(
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
        CloudEncryptionKey cloudEncryptionKey = (CloudEncryptionKey) o;
        return id == cloudEncryptionKey.id &&
                siteId == cloudEncryptionKey.siteId &&
                activates == cloudEncryptionKey.activates &&
                created == cloudEncryptionKey.created &&
                Objects.equals(secret, cloudEncryptionKey.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteId, activates, created, secret);
    }
}
