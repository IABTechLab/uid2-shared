// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.secure;

import com.uid2.shared.secure.nitro.AttestationDocument;
import com.uid2.shared.secure.nitro.AttestationRequest;
import com.uid2.shared.secure.IAttestationProvider;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class NitroAttestationProvider implements IAttestationProvider {

    private Set<NitroEnclaveIdentifier> allowedEnclaveIds;
    private ICertificateProvider certificateProvider;

    public NitroAttestationProvider(ICertificateProvider certificateProvider) {
        this.allowedEnclaveIds = new HashSet<>();
        this.certificateProvider = certificateProvider;
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        try {
            AttestationRequest aReq = AttestationRequest.createFrom(attestationRequest);
            AttestationDocument aDoc = aReq.getAttestationDocument();
            handler.handle(Future.succeededFuture(attestInternal(publicKey, aReq, aDoc)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(new AttestationException(e)));
        }
    }

    private AttestationResult attestInternal(byte[] publicKey, AttestationRequest aReq, AttestationDocument aDoc) throws Exception {
        if (!aReq.verifyData()) {
            return new AttestationResult(AttestationFailure.BAD_PAYLOAD);
        }

        if (publicKey != null && publicKey.length > 0 && !Arrays.equals(publicKey, aDoc.getPublicKey())) {
            return new AttestationResult(AttestationFailure.BAD_PAYLOAD);
        }

        if (!aReq.verifyCertChain(certificateProvider.getRootCertificate())) {
            return new AttestationResult(AttestationFailure.BAD_CERTIFICATE);
        }

        NitroEnclaveIdentifier id = NitroEnclaveIdentifier.fromRaw(aDoc.getPcr(0));
        if (!allowedEnclaveIds.contains(id)) {
            return new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE);
        }

        return new AttestationResult(aDoc.getPublicKey());
    }

    @Override
    public void registerEnclave(String encodedIdentifier) throws AttestationException {
        try {
            // we don't use raw bytes, just checking if it's valid b64
            Base64.getDecoder().decode(encodedIdentifier);
            this.addIdentifier(NitroEnclaveIdentifier.fromBase64(encodedIdentifier));
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public void unregisterEnclave(String encodedIdentifier) throws AttestationException {
        try {
            // we don't use raw bytes, just checking if it's valid b64
            Base64.getDecoder().decode(encodedIdentifier);
            this.removeIdentifier(NitroEnclaveIdentifier.fromBase64(encodedIdentifier));
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public Collection<String> getEnclaveAllowlist() {
        return this.allowedEnclaveIds.stream().map(id -> id.toString()).collect(Collectors.toList());
    }

    public void addIdentifier(NitroEnclaveIdentifier id) {
        this.allowedEnclaveIds.add(id);
    }

    public void removeIdentifier(NitroEnclaveIdentifier id) {
        this.allowedEnclaveIds.remove(id);
    }

}
