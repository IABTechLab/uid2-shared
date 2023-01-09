package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public class OperatorKey implements IRoleAuthorizable<Role> {
    private String key;
    private final String name;
    private final String contact;
    private final String protocol;
    // epochSeconds
    private final long created;
    private boolean disabled;
    @JsonProperty("site_id")
    private Integer siteId;
    private boolean publicOperator;

    // UID2-598 for initial rollout, we will default every operator to be public and then manually change to private for
    // appropriate ones and later we can default back to private operator again
    private static boolean defaultPublicOperatorStatus = true;

    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = null;
        this.publicOperator = defaultPublicOperatorStatus;
    }

    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled, Integer siteId) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = siteId;
        this.publicOperator = defaultPublicOperatorStatus;
    }

    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled, Integer siteId, boolean publicOperator) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = siteId;
        this.publicOperator = publicOperator;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getProtocol() { return protocol; }
    public long getCreated() { return created; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Integer getSiteId() { return siteId; }
    public boolean isPublicOperator() { return publicOperator; }
    public boolean isPrivateOperator() { return !publicOperator; }
    public void setPublicOperator(boolean enabled) { this.publicOperator = enabled; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }

    public static OperatorKey valueOf(JsonObject json) {
        return new OperatorKey(
                json.getString("key"),
                json.getString("name"),
                json.getString("contact"),
                json.getString("protocol"),
                json.getLong("created"),
                json.getBoolean("disabled", false),
                json.getInteger("site_id"),
                json.getBoolean("publicOperator", defaultPublicOperatorStatus));
    }

    @Override
    public boolean hasRole(Role role) {
        return role == Role.OPERATOR;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        // If the object is of a different type, return false
        if (!(o instanceof OperatorKey)) return false;

        OperatorKey b = (OperatorKey) o;

        // Compare the data members and return accordingly
        return this.key.equals(b.key)
                && this.name.equals(b.name)
                && this.contact.equals(b.contact)
                && this.protocol.equals(b.protocol)
                && this.created == b.created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, contact, protocol, created);
    }

    public void setKey(String newKey) {
        this.key = newKey;
    }
}
