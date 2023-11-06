package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Site {
    private static final String DEFAULT_DESCRIPTION = "";
    private static final boolean DEFAULT_VISIBLE = true;
    private final int id;
    private final String name;
    private String description;
    private Boolean enabled;
    @JsonProperty("domain_names")
    private Set<String> domainNames;

    private Set<ClientType> clientTypes;
    private Boolean visible;
    private final long created;

    @JsonCreator
    public Site(@JsonProperty("id") int id,
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("enabled") Boolean enabled,
                @JsonProperty("clientTypes") Set<ClientType> types,
                @JsonProperty("domains") Set<String> domains,
                @JsonProperty("visible") Boolean visible,
                @JsonProperty("created") long created) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.clientTypes = (types != null) ? new HashSet<>(types) : new HashSet<>();
        this.domainNames = (domains != null) ? new HashSet<>(domains) : new HashSet<>();
        this.visible = visible;
        this.created = created;
    }

    public Site(int id, String name, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = DEFAULT_DESCRIPTION;
        this.enabled = enabled;
        this.domainNames = new HashSet<>();
        this.clientTypes = new HashSet<>();
        this.visible = DEFAULT_VISIBLE;
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<String> domains) {
        this.id = id;
        this.name = name;
        this.description = DEFAULT_DESCRIPTION;
        this.enabled = enabled;
        this.domainNames = (domains != null) ? new HashSet<>(domains) : new HashSet<>();
        this.clientTypes = new HashSet<>();
        this.visible = DEFAULT_VISIBLE;
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains) {
        this.id = id;
        this.name = name;
        this.description = DEFAULT_DESCRIPTION;
        this.enabled = enabled;
        this.clientTypes = (types != null) ? new HashSet<>(types) : new HashSet<>();
        this.domainNames = (domains != null) ? new HashSet<>(domains) : new HashSet<>();
        this.visible = DEFAULT_VISIBLE;
        this.created = Instant.now().getEpochSecond();
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains, long created) {
        this.id = id;
        this.name = name;
        this.description = DEFAULT_DESCRIPTION;
        this.enabled = enabled;
        this.clientTypes = (types != null) ? new HashSet<>(types) : new HashSet<>();
        this.domainNames = (domains != null) ? new HashSet<>(domains) : new HashSet<>();
        this.visible = DEFAULT_VISIBLE;
        this.created = created;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean isEnabled() { return enabled; }
    public Set<String> getDomainNames() { return domainNames; }
    public Set<ClientType> getClientTypes() { return clientTypes; }
    public Boolean isVisible() { return visible; }

    public long getCreated() { return  created; }

    public void setDescription(String description) { this.description = description; }

    public void setClientTypes(Set<ClientType> clientTypes) { this.clientTypes = clientTypes; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setDomainNames(Set<String> domainNames) { this.domainNames = domainNames; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description=" + description +
                ", enabled=" + enabled +
                ", domain_names=" + domainNames.toString() +
                ", clientTypes=" + clientTypes.toString() +
                ", visible=" + visible +
                ", created=" + String.valueOf(created) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return id == site.id && name.equals(site.name) && Objects.equals(description, site.description) && enabled.equals(site.enabled) && domainNames.equals(site.domainNames) && clientTypes.equals(site.clientTypes) && visible.equals(site.visible) && created == site.created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, enabled, domainNames,Arrays.hashCode(clientTypes.toArray()), visible, created);
    }
}