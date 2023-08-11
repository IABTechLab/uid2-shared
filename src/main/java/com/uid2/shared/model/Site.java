package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Site {
    private final int id;
    private final String name;
    private Boolean enabled;
    @JsonProperty("domain_names")
    private Set<String> domainNames;

    public Site(int id, String name, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.domainNames = new HashSet<>();
    }

    public Site(int id, String name, Boolean enabled, Set<String> domains) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.domainNames = domains;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Boolean isEnabled() { return enabled; }
    public Set<String> getDomainNames() { return domainNames; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setDomainNames(Set<String> domainNames) { this.domainNames = domainNames; }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return id == site.id && name.equals(site.name) && enabled.equals(site.enabled) && domainNames.equals(site.domainNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, domainNames);
    }
}