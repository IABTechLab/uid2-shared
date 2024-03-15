package com.uid2.shared.secure;

import java.util.Collection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface ICoreAttestationService {
    void attest(
        byte[] attestationRequest,
        byte[] publicKey,
        Handler<AsyncResult<AttestationResult>> handler
    );

    void registerEnclave(String encodedIdentifier) throws AttestationException;
    void unregisterEnclave(String encodedIdentifier) throws AttestationException;

    Collection<String> getEnclaveAllowlist();
}
