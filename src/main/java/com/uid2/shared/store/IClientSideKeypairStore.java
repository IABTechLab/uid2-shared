package com.uid2.shared.store;

import com.uid2.shared.model.ClientSideKeypair;

import java.time.Instant;
import java.util.List;

public interface IClientSideKeypairStore {
    public IClientSideKeypairStoreSnapshot getSnapshot(Instant asOf);
    public IClientSideKeypairStoreSnapshot getSnapshot();

    public interface IClientSideKeypairStoreSnapshot {
        public List<ClientSideKeypair> getAll();
        public ClientSideKeypair getKeypair(String subscriptionId);
        public List<ClientSideKeypair> getSiteKeypairs(int siteId);
        public List<ClientSideKeypair> getEnabledKeypairs();
    }
}
