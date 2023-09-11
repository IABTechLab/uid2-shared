package com.uid2.shared.store;

import com.uid2.shared.model.Service;

import java.util.Collection;

public interface IServiceStore {

    Collection<Service> getAllServices();

    Service getService(int serviceId);
}
