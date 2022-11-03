package com.uid2.shared.auth;


import com.uid2.shared.model.EnclaveIdentifier;

import java.util.Collection;

public interface IEnclaveIdentifierProvider {
    Collection<EnclaveIdentifier> getAll();
    void addListener(IOperatorChangeHandler handler) throws Exception;
    void removeListener(IOperatorChangeHandler handler);
}
