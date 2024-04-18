package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
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
        setAppNames(appNames);
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

    public Boolean isEnabled() { return enabled; }
    public Boolean isVisible() { return visible; }
    public void setAppNames(Set<String> appNames) { this.appNames = (appNames != null) ? appNames : new HashSet<>(); }
}