package com.uid2.shared.model;

import com.uid2.shared.auth.Role;

import java.util.Objects;
import java.util.Set;

public class Service {
    private final int serviceId;
    private final int siteId;
    private final String name;
    private Set<Role> roles;

    public Service(int serviceId, int siteId, String name, Set<Role> roles) {
        this.serviceId = serviceId;
        this.siteId = siteId;
        this.name = name;
        this.roles = roles;
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

    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "Service{" +
                "serviceId=" + serviceId +
                ", siteId=" + siteId +
                ", name='" + name + '\'' +
                ", roles=" + roles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return serviceId == service.serviceId && siteId == service.siteId && name.equals(service.name) && roles.equals(service.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, siteId, name, roles.hashCode());
    }
}
