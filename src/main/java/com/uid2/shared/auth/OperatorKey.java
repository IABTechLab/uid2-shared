package com.uid2.shared.auth;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OperatorKey implements IRoleAuthorizable<Role> {
    private String key;
    private final String name;
    private final String contact;
    private final String protocol;
    // epochSeconds
    private final long created;
    private boolean disabled;
    private final List<Integer> siteIds = new ArrayList<>();

    public OperatorKey(String key, String name, String contact, String protocol, long created, boolean disabled, List<Integer> siteIds) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteIds.addAll(siteIds);
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getProtocol() { return protocol; }
    public long getCreated() { return created; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public List<Integer> getSiteIds() { return siteIds; }

    public static OperatorKey valueOf(JsonObject json) {
        return new OperatorKey(
                json.getString("key"),
                json.getString("name"),
                json.getString("contact"),
                json.getString("protocol"),
                json.getLong("created"),
                json.getBoolean("disabled", false),
                json.getJsonArray("site_ids") != null ? json.getJsonArray("site_ids").stream().map(x -> (int) x).collect(Collectors.toList()) : new ArrayList<>());
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
                && this.created == b.created
                && this.siteIds.equals(b.siteIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, contact, protocol, created, siteIds);
    }

    public void setKey(String newKey) {
        this.key = newKey;
    }
}
