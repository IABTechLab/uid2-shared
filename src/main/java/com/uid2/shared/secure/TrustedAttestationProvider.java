package com.uid2.shared.secure;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.Collection;
import java.util.Collections;

public class TrustedAttestationProvider implements IAttestationProvider {
    public TrustedAttestationProvider() {}

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        handler.handle(Future.succeededFuture(new AttestationResult(publicKey, "Trusted")));
    }

    @Override
    public void registerEnclave(String encodedIdentifier) throws AttestationException {}
    @Override
    public void unregisterEnclave(String encodedIdentifier) throws AttestationException {}
    @Override
    public Collection<String> getEnclaveAllowlist() { return Collections.emptyList(); }
}