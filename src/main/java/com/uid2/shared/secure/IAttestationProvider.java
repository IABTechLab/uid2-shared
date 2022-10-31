package com.uid2.shared.secure;

import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationResult;

import java.util.Collection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface IAttestationProvider {
    void attest(
        byte[] attestationRequest,
        byte[] publicKey,
        Handler<AsyncResult<AttestationResult>> handler
    );

    void registerEnclave(String encodedIdentifier) throws AttestationException;
    void unregisterEnclave(String encodedIdentifier) throws AttestationException;

    Collection<String> getEnclaveAllowlist();
}
