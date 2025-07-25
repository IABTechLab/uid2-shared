package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;
import com.uid2.shared.auth.Role;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceLink {
    @JsonProperty("site_id")
    private final int siteId;
    @JsonProperty("service_id")
    private final int serviceId;
    @JsonProperty("link_id")
    private String linkId;
    private String name;
    private Set<Role> roles;
    private boolean disabled;

    public ServiceLink(String linkId, int serviceId, int siteId, String name, Set<Role> roles) {
        this(linkId, serviceId, siteId, name, roles, false);
    }

    @JsonCreator
    public ServiceLink(
            @JsonProperty("link_id") String linkId,
            @JsonProperty("service_id") int serviceId,
            @JsonProperty("site_id") int siteId,
            @JsonProperty("name") String name,
            @JsonProperty("roles") Set<Role> roles,
            @JsonProperty("disabled") boolean disabled) {
        this.linkId = linkId;
        this.serviceId = serviceId;
        this.siteId = siteId;
        this.name = name;
        this.roles = Objects.requireNonNullElseGet(roles, HashSet::new);
        this.disabled = disabled;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public int getSiteId() {
        return siteId;
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
        return "ServiceLink{" +
                "siteId=" + siteId +
                ", serviceId=" + serviceId +
                ", linkId='" + linkId + '\'' +
                ", name='" + name + '\'' +
                ", roles=" + roles +
                ", disabled=" + disabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLink serviceLink = (ServiceLink) o;
        return siteId == serviceLink.siteId 
            && serviceId == serviceLink.serviceId 
            && linkId.equals(serviceLink.linkId)
            && name.equals(serviceLink.name) 
            && roles.equals(serviceLink.roles)
            && disabled == serviceLink.disabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, serviceId, linkId, name, roles.hashCode(), disabled);
    }
}
