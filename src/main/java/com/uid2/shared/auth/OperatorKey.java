package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OperatorKey implements IRoleAuthorizable<Role> {
    private static final OperatorType DEFAULT_OPERATOR_TYPE = OperatorType.PRIVATE;

    @JsonProperty("key_hash")
    private final String keyHash;
    @JsonProperty("key_salt")
    private final String keySalt;
    private final String name;
    private final String contact;
    private final String protocol;
    private final long created; // epochSeconds
    private boolean disabled;
    @JsonProperty("site_id")
    private Integer siteId;
    private Set<Role> roles;
    @JsonProperty("operator_type")
    private OperatorType operatorType;
    @JsonProperty("key_id")
    private String keyId;

    @JsonCreator
    public OperatorKey(
            @JsonProperty("key_hash") String keyHash,
            @JsonProperty("key_salt") String keySalt,
            @JsonProperty("name") String name,
            @JsonProperty("contact") String contact,
            @JsonProperty("protocol") String protocol,
            @JsonProperty("created") long created,
            @JsonProperty("disabled") boolean disabled,
            @JsonProperty("site_id") Integer siteId,
            @JsonProperty("roles") Set<Role> roles,
            @JsonProperty("operator_type") OperatorType operatorType,
            @JsonProperty("key_id") String keyId) {
        this.keyHash = keyHash;
        this.keySalt = keySalt;
        this.name = name;
        this.contact = contact;
        this.protocol = protocol;
        this.created = created;
        this.disabled = disabled;
        this.siteId = siteId;
        this.roles = this.reorderAndAddDefaultRole(roles);
        this.operatorType = operatorType == null ? DEFAULT_OPERATOR_TYPE : operatorType;
        this.keyId = keyId;
    }

    public OperatorKey(String keyHash, String keySalt, String name, String contact, String protocol, long created, boolean disabled, Integer siteId, Set<Role> roles, String keyId) {
        this(keyHash, keySalt, name, contact, protocol, created, disabled, siteId, roles, DEFAULT_OPERATOR_TYPE, keyId);
    }

    public OperatorKey(String keyHash, String keySalt, String name, String contact, String protocol, long created, boolean disabled, Integer siteId, String keyId) {
        this(keyHash, keySalt, name, contact, protocol, created, disabled, siteId, Set.of(Role.OPERATOR), DEFAULT_OPERATOR_TYPE, keyId);
    }

    public OperatorKey(String keyHash, String keySalt, String name, String contact, String protocol, long created, boolean disabled, String keyId) {
        this(keyHash, keySalt, name, contact, protocol, created, disabled, null, Set.of(Role.OPERATOR), DEFAULT_OPERATOR_TYPE, keyId);
    }

    @Override
    public String getKeyHash() {
        return keyHash;
    }

    @Override
    public String getKeySalt() {
        return keySalt;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getContact() {
        return contact;
    }

    public String getProtocol() {
        return protocol;
    }

    public long getCreated() {
        return created;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public Set<Role> getRoles() {
        return roles;
    }
    @Override
    public String getKeyId() {return keyId; }

    @Override
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public void setRoles(Set<Role> roles) {
        this.roles = this.reorderAndAddDefaultRole(roles);
    }

    public OperatorKey withRoles(Set<Role> roles) {
        setRoles(roles);
        return this;
    }

    public OperatorKey withRoles(Role... roles) {
        setRoles(new TreeSet<>(Arrays.asList(roles)));
        return this;
    }

    private Set<Role> reorderAndAddDefaultRole(Set<Role> roles) {
        Set<Role> newRoles = roles != null ? new TreeSet<>(roles) : new TreeSet<>();
        newRoles.removeIf(Objects::isNull);
        if (!newRoles.contains(Role.OPTOUT_SERVICE)) {
            newRoles.add(Role.OPERATOR);
        }

        return Collections.unmodifiableSet(newRoles);
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(OperatorType type) {
        this.operatorType = type;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        // If the object is of a different type, return false
        if (!(o instanceof OperatorKey)) return false;

        OperatorKey b = (OperatorKey) o;

        // Compare the data members and return accordingly
        return this.keyHash.equals(b.keyHash)
                && this.keySalt.equals(b.keySalt)
                && this.name.equals(b.name)
                && this.contact.equals(b.contact)
                && this.protocol.equals(b.protocol)
                && this.disabled == b.disabled
                && Objects.equals(this.siteId, b.siteId)
                && this.roles.equals(b.roles)
                && this.created == b.created
                && this.operatorType == b.operatorType
                && this.keyId.equals(keyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyHash, keySalt, name, contact, protocol, created, disabled, siteId, roles, operatorType, keyId);
    }
}
