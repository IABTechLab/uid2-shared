package com.uid2.shared.store;

import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.IAuthorizableProvider;

import java.util.Collection;

public interface IClientKeyProvider extends IAuthorizableProvider {
    ClientKey getClientKey(String token);
    ClientKey getClientKeyFromHash(String hash);
    Collection<ClientKey> getAll();
}
