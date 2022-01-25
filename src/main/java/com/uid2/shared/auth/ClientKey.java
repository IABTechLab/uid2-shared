// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uid2.shared.model.SiteUtil;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ClientKey implements IRoleAuthorizable<Role> {
    private String key;
    private String name;
    private String contact;
    private long created;
    private Set<Role> roles;
    @JsonProperty("site_id")
    private int siteId;
    private boolean disabled;

    public ClientKey(String key) {
        this.key = key;
        created = Instant.parse("2021-01-01T00:00:00.000Z").getEpochSecond();
        siteId = -1;
    }

    public ClientKey(String key, Instant created) {
        this.key = key;
        this.created = created.getEpochSecond();
        this.siteId = -1;
    }

    public ClientKey withName(String name) { this.name = name; return this; }
    public ClientKey withContact(String contact) { this.contact = contact; return this; }
    public ClientKey withNameAndContact(String name) { this.name = this.contact = name; return this; }
    public ClientKey withRoles(Role... roles) { this.roles = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles))); return this; }
    public ClientKey withRoles(Set<Role> roles) { this.roles = Collections.unmodifiableSet(roles); return this; }
    public ClientKey withSiteId(int siteId) { this.siteId = siteId; return this; }

    public ClientKey(String key, String contact, Role... roles) {
        this(key, contact, contact, Instant.parse("2021-01-01T00:00:00.000Z"), new HashSet<Role>(Arrays.asList(roles)), 0, false);
    }

    public ClientKey(String key, String name, String contact, Instant created, Set<Role> roles, int siteId,
                     boolean disabled) {
        this.key = key;
        this.name = name;
        this.contact = contact;
        this.created = created.getEpochSecond();
        this.roles = Collections.unmodifiableSet(roles);
        this.siteId = siteId;
        this.disabled = disabled;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    public long getCreated() {
        return created;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public int getSiteId() {
        return siteId;
    }
    public boolean hasValidSiteId() { return SiteUtil.isValidSiteId(siteId); }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }

    // Overriding equals() to compare two ClientKey objects
    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) return true;

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof ClientKey)) return false;

        // typecast o to Complex so that we can compare data members
        ClientKey b = (ClientKey) o;

        // Compare the data members and return accordingly
        return this.key.equals(b.key)
            && this.name.equals(b.name)
            && this.contact.equals(b.contact)
            && this.roles.equals(this.roles)
            && this.created == b.created
            && this.siteId == b.siteId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(created, key, name, contact, roles, siteId);
    }

    public static ClientKey valueOf(JsonObject json) {
        return new ClientKey(
            json.getString("key"),
            json.getString("name"),
            json.getString("contact"),
            Instant.ofEpochSecond(json.getLong("created")),
            Roles.getRoles(Role.class, json),
            json.getInteger("site_id"),
            json.getBoolean("disabled", false)
        );
    }

    @Override
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public void setKey(String newKey) {
        this.key = newKey;
    }
}
