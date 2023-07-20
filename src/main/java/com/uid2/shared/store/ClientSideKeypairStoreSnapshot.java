package com.uid2.shared.store;

import com.uid2.shared.model.ClientSideKeypair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientSideKeypairStoreSnapshot implements IClientSideKeypairStore.IClientSideKeypairStoreSnapshot {
    private final HashMap<String, ClientSideKeypair> keypairMap;
    private final HashMap<Integer, List<ClientSideKeypair>> siteKeypairMap;
    private final List<ClientSideKeypair> enabledKeypairs;

    public ClientSideKeypairStoreSnapshot(HashMap<String, ClientSideKeypair> keypairMap, HashMap<Integer, List<ClientSideKeypair>> siteKeypairMap){
        this.keypairMap = keypairMap;
        this.siteKeypairMap = siteKeypairMap;
        this.enabledKeypairs = new ArrayList<>();

        for(Map.Entry<String, ClientSideKeypair> entry : keypairMap.entrySet()) {
            ClientSideKeypair k = entry.getValue();
            if(!k.isDisabled()){
                this.enabledKeypairs.add(k);
            }
        }
    }

    @Override
    public List<ClientSideKeypair> getAll() {
        return new ArrayList<>(keypairMap.values());
    }

    @Override
    public ClientSideKeypair getKeypair(String subscriptionId) {
        try {
            return this.keypairMap.get(subscriptionId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Subscription ID " + subscriptionId + " not supported");
        }
    }

    @Override
    public List<ClientSideKeypair> getSiteKeypairs(int sideId) {
        try {
            return siteKeypairMap.get(sideId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Site ID " + String.valueOf(sideId) + " not supported");
        }
    }

    @Override
    public List<ClientSideKeypair> getEnabledKeypairs() {
        return this.enabledKeypairs;
    }


}
