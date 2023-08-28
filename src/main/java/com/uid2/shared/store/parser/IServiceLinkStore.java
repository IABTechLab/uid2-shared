package com.uid2.shared.store.parser;

import com.uid2.shared.model.ServiceLink;

import java.util.Collection;

public interface IServiceLinkStore {
    Collection<ServiceLink> getAllServiceLinks();

    ServiceLink getServiceLink(String linkId);
}
