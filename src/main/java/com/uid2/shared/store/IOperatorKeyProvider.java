package com.uid2.shared.store;

import com.uid2.shared.auth.IAuthorizableProvider;
import com.uid2.shared.auth.OperatorKey;

import java.util.Collection;

public interface IOperatorKeyProvider extends IAuthorizableProvider {
    OperatorKey getOperatorKey(String token);
    Collection<OperatorKey> getAll();
}
