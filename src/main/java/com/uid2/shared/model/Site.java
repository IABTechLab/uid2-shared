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
    @JsonProperty("app_names")
    private Set<String> appNames;
    private Set<ClientType> clientTypes;
    private Boolean visible;
    private final long created;

    @JsonCreator
    public Site(@JsonProperty("id") int id,
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("enabled") Boolean enabled,
                @JsonProperty("clientTypes") Set<ClientType> types,
                @JsonProperty("domain_names") Set<String> domains,
                @JsonProperty("app_names") Set<String> appNames,
                @JsonProperty("visible") Boolean visible,
                @JsonProperty("created") long created) {
        this.id = id;
        this.name = name;
        this.description = (description != null) ? description : DEFAULT_DESCRIPTION;
        this.enabled = enabled;
        this.clientTypes = (types != null) ? new HashSet<>(types) : new HashSet<>();
        this.domainNames = (domains != null) ? new HashSet<>(domains) : new HashSet<>();
        this.appNames = (appNames != null) ? new HashSet<>(appNames) : new HashSet<>();
        this.visible = visible;
        this.created = created;
    }

    public Site(int id, String name, Boolean enabled) {
        this(id, name, enabled, new HashSet<>());
    }

    public Site(int id, String name, Boolean enabled, Set<String> domains) {
        this(id, name, enabled, new HashSet<>(), domains);
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains) {
        this(id, name, enabled, types, domains, Instant.now().getEpochSecond());
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains, long created) {
        this(id, name, DEFAULT_DESCRIPTION, enabled, types, domains, new HashSet<>(), DEFAULT_VISIBLE, created);
    }

    public Site(int id, String name, String description, Boolean enabled, Set<ClientType> types, Set<String> domains, Boolean visible) {
        this(id, name, description, enabled, types, domains, new HashSet<>(), visible);
    }

    public Site(int id, String name, Boolean enabled, Set<ClientType> types, Set<String> domains, Set<String> appNames) {
        this(id, name, DEFAULT_DESCRIPTION, enabled, types, domains, appNames, DEFAULT_VISIBLE);
    }

    public Site(int id, String name, String description, Boolean enabled, Set<ClientType> types, Set<String> domains, Set<String> appNames, Boolean visible) {
        this(id, name, description, enabled, types, domains, appNames, visible, Instant.now().getEpochSecond());
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean isEnabled() { return enabled; }
    public Set<String> getDomainNames() { return domainNames; }
    public Set<String> getAppNames() { return appNames; }
    public Set<ClientType> getClientTypes() { return clientTypes; }
    public Boolean isVisible() { return visible; }

    public long getCreated() { return  created; }

    public void setDescription(String description) { this.description = description; }

    public void setClientTypes(Set<ClientType> clientTypes) { this.clientTypes = clientTypes; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setDomainNames(Set<String> domainNames) { this.domainNames = domainNames; }
    public void setAppNames(Set<String> appNames) { this.appNames = appNames; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description=" + description +
                ", enabled=" + enabled +
                ", domain_names=" + domainNames.toString() +
                ", app_names=" + appNames.toString() +
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
        return id == site.id && name.equals(site.name) && description.equals(site.description) && enabled.equals(site.enabled) && appNames.equals(site.appNames) && clientTypes.equals(site.clientTypes) && Objects.equals(visible, site.visible) && created == site.created && domainNames.equals(site.domainNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, enabled, domainNames, appNames, Arrays.hashCode(clientTypes.toArray()), visible, created);
    }
}