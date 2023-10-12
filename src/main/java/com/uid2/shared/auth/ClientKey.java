package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uid2.shared.Utils;
import com.uid2.shared.model.SiteUtil;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientKey implements IRoleAuthorizable<Role> {
    @JsonProperty("key_hash")
    private final String keyHash;
    @JsonProperty("key_salt")
    private final String keySalt;
    private final String secret;
    private final byte[] secretBytes;
    private String name;
    private String contact;
    private final long created; // epochSeconds
    private Set<Role> roles;
    @JsonProperty("site_id")
    private int siteId;
    private boolean disabled;
    @JsonProperty("service_id")
    private int serviceId;
    @JsonProperty("key_id")
    private String keyId;

    @JsonCreator
    public ClientKey(
            @JsonProperty("key_hash") String keyHash,
            @JsonProperty("key_salt") String keySalt,
            @JsonProperty("secret") String secret,
            @JsonProperty("name") String name,
            @JsonProperty("contact") String contact,
            @JsonProperty("created") long created,
            @JsonProperty("roles") Set<Role> roles,
            @JsonProperty("site_id") int siteId,
            @JsonProperty("disabled") boolean disabled,
            @JsonProperty("service_id") int serviceId,
            @JsonProperty("key_id") String keyId) {
        this.keyHash = keyHash;
        this.keySalt = keySalt;
        this.secret = secret;
        this.secretBytes = Utils.decodeBase64String(secret);
        this.name = name;
        this.contact = contact;
        this.created = created;
        this.roles = Collections.unmodifiableSet(roles);
        this.siteId = siteId;
        this.disabled = disabled;
        this.serviceId = serviceId;
        this.keyId = keyId;
    }

    public ClientKey(String keyHash, String keySalt, String secret, String name, String contact, Instant created, Set<Role> roles, int siteId, boolean disabled, String keyId) {
        this(keyHash, keySalt, secret, name, contact, created.getEpochSecond(), roles, siteId, disabled, 0, keyId);
    }

    public ClientKey(String keyHash, String keySalt, String secret, String name, Instant created, Set<Role> roles, int siteId, boolean disabled, String keyId) {
        this(keyHash, keySalt, secret, name, name, created.getEpochSecond(), roles, siteId, disabled, 0, keyId);
    }

    public ClientKey(String keyHash, String keySalt, String secret, String name, Instant created, Set<Role> roles, int siteId, String keyId) {
        this(keyHash, keySalt, secret, name, name, created.getEpochSecond(), roles, siteId, false, 0, keyId);
    }

    @Override
    public String getKeyHash() {
        return keyHash;
    }

    @Override
    public String getKeySalt() {
        return keySalt;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public String getKeyId() { return keyId; }

    @JsonIgnore
    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public String getName() {
        return name;
    }

    public ClientKey withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getContact() {
        return contact;
    }

    public ClientKey withContact(String contact) {
        this.contact = contact;
        return this;
    }

    public ClientKey withNameAndContact(String name) {
        this.name = this.contact = name;
        return this;
    }

    public long getCreated() {
        return created;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public ClientKey withRoles(Role... roles) {
        this.roles = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles)));
        return this;
    }

    public ClientKey withRoles(Set<Role> roles) {
        this.roles = Collections.unmodifiableSet(roles);
        return this;
    }

    @Override
    public Integer getSiteId() {
        return siteId;
    }

    public boolean hasValidSiteId() {
        return SiteUtil.isValidSiteId(siteId);
    }

    public ClientKey withSiteId(int siteId) {
        this.siteId = siteId;
        return this;
    }

    public ClientKey withServiceId(Integer serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getServiceId() {
        return serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof ClientKey)) return false;

        ClientKey b = (ClientKey) o;

        return this.keyHash.equals(b.keyHash)
                && this.keySalt.equals(b.keySalt)
                && this.secret.equals(b.secret)
                && this.name.equals(b.name)
                && this.contact.equals(b.contact)
                && this.roles.equals(b.roles)
                && this.created == b.created
                && this.siteId == b.siteId
                && this.disabled == b.disabled
                && Arrays.equals(this.secretBytes, b.secretBytes)
                && this.serviceId == b.serviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyHash, keySalt, secret, name, contact, roles, created, siteId, disabled, Arrays.hashCode(secretBytes), serviceId);
    }
}
