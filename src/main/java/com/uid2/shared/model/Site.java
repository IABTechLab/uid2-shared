package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Site {
    private final int id;
    private final String name;
    private Boolean enabled;
    @JsonProperty("domain_names")
    private Set<String> domainNames;

    private Set<ClientType> clientTypes;
    private final long created;

    public Site(int id, String name, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.domainNames = new HashSet<>();
        this.clientTypes = new HashSet<>();
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<String> domains) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.domainNames = domains;
        this.clientTypes = new HashSet<>();
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.clientTypes = types;
        this.domainNames = domains;
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains, long created) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.clientTypes = types;
        this.domainNames = domains;
        this.created = created;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Boolean isEnabled() { return enabled; }
    public Set<String> getDomainNames() { return domainNames; }
    public Set<ClientType> getClientTypes() { return clientTypes; }
    public long getCreated() { return  created; }
    public void setClientTypes(Set<ClientType> clientTypes) { this.clientTypes = clientTypes; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setDomainNames(Set<String> domainNames) { this.domainNames = domainNames; }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", domain_names=" + domainNames.toString() +
                ", clientTypes=" + clientTypes.toString() +
                ", created=" + String.valueOf(created) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return id == site.id && name.equals(site.name) && enabled.equals(site.enabled) && domainNames.equals(site.domainNames) && clientTypes.equals(site.clientTypes) && created == site.created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, domainNames,Arrays.hashCode(clientTypes.toArray()), created);
    }
}