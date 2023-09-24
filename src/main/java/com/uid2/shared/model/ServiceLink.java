package com.uid2.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ServiceLink {
    @JsonProperty("site_id")
    private final int siteId;
    @JsonProperty("service_id")
    private final int serviceId;
    @JsonProperty("link_id")
    private String linkId;
    private String name;

    public ServiceLink(String linkId, int serviceId, int siteId, String name) {
        this.linkId = linkId;
        this.serviceId = serviceId;
        this.siteId = siteId;
        this.name = name;
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
