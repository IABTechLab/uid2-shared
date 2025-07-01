package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uid2.shared.auth.Role;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Service {
    @JsonProperty("service_id")
    private final int serviceId;
    @JsonProperty("site_id")
    private int siteId;
    @JsonProperty("link_id_regex")
    private String linkIdRegex;
    private String name;
    private Set<Role> roles;
    private boolean disabled;

    public Service(int serviceId, int siteId, String name, Set<Role> roles) {
        this(serviceId, siteId, name, roles, null, false);
    }

    public Service(int serviceId, int siteId, String name, Set<Role> roles, String linkIdRegex) {
        this(serviceId, siteId, name, roles, linkIdRegex, false);
    }

    public Service(int serviceId, int siteId, String name, Set<Role> roles, String linkIdRegex, boolean disabled) {
        this.serviceId = serviceId;
        this.siteId = siteId;
        this.name = name;
        this.roles = Objects.requireNonNullElseGet(roles, HashSet::new);
        this.linkIdRegex = linkIdRegex;
        this.disabled = disabled;
    }

    public int getServiceId() {
        return serviceId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getLinkIdRegex() {
        return linkIdRegex;
    }

    public void setLinkIdRegex(String linkIdRegex) {
        this.linkIdRegex = linkIdRegex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = Objects.requireNonNullElseGet(roles, HashSet::new);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String toString() {
        return "Service{" +
                "serviceId=" + serviceId +
                ", siteId=" + siteId +
                ", name='" + name + '\'' +
                ", roles=" + roles +
                ", linkIdRegex=" + linkIdRegex +
                ", disabled=" + disabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service other = (Service) o;

        return serviceId == other.serviceId
            && siteId == other.siteId
            && disabled == other.disabled
            && Objects.equals(name,        other.name)
            && Objects.equals(roles,       other.roles)
            && Objects.equals(linkIdRegex, other.linkIdRegex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, siteId, name, roles.hashCode(), linkIdRegex, disabled);
    }
}
