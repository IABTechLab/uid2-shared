package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.*;

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
    private Set<Role> roles;

    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = null;
        this.roles = new HashSet<>(Arrays.asList(Role.OPERATOR));
    }
    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled, Integer siteId) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = siteId;
        this.roles = new HashSet<>(Arrays.asList(Role.OPERATOR));
    }
    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled, Integer siteId, Set<Role> roles) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = siteId;
        roles.add(Role.OPERATOR);
        this.roles = Collections.unmodifiableSet(roles);
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getProtocol() { return protocol; }
    public long getCreated() { return created; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Set<Role> getRoles() {
        return roles;
    }

    public Integer getSiteId() { return siteId; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }
    public void setRoles(Set<Role> roles) {
        roles.add(Role.OPERATOR);
        this.roles = Collections.unmodifiableSet(roles);
    }
    public OperatorKey withRoles(Set<Role> roles) { setRoles(roles); return this; }
    public OperatorKey withRoles(Role... roles) { setRoles(new HashSet<>(Arrays.asList(roles))); return this; }
    public static OperatorKey valueOf(JsonObject json) {
        return new OperatorKey(
                json.getString("key"),
                json.getString("name"),
                json.getString("contact"),
                json.getString("protocol"),
                json.getLong("created"),
                json.getBoolean("disabled", false),
                json.getInteger("site_id"),
                Roles.getRoles(Role.class, json));
    }

    @Override
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
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
                && this.disabled == b.disabled
                && this.siteId.equals(b.siteId)
                && this.roles.equals(this.roles)
                && this.created == b.created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, contact, protocol, created, disabled, siteId, roles);
    }

    public void setKey(String newKey) {
        this.key = newKey;
    }
}
