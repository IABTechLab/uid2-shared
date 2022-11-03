package com.uid2.shared.auth;

import com.uid2.shared.model.EnclaveIdentifier;

import java.util.Set;

public interface IOperatorChangeHandler {
    void handle(Set<EnclaveIdentifier> newSet);
}
