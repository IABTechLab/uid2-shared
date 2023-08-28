package com.uid2.shared.model;

import java.util.Objects;

public class ServiceLink {
    private final int siteId;
    private final int serviceId;
    private final String linkId;
    private final String name;

    public ServiceLink(int siteId, int serviceId, String linkId, String name) {
        this.siteId = siteId;
        this.serviceId = serviceId;
        this.linkId = linkId;
        this.name = name;
    }

    public int getSiteId() {
        return siteId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getLinkId() {
        return linkId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ServiceLink{" +
                "siteId=" + siteId +
                ", serviceId=" + serviceId +
                ", linkId='" + linkId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLink serviceLink = (ServiceLink) o;
        return siteId == serviceLink.siteId && serviceId == serviceLink.serviceId && linkId.equals(serviceLink.linkId) && name.equals(serviceLink.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, serviceId, linkId, name);
    }
}
