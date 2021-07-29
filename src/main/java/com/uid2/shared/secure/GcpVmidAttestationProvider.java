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

import com.google.auth.oauth2.GoogleCredentials;
import com.uid2.shared.Utils;
import com.uid2.shared.secure.gcp.VmConfigVerifier;
import com.uid2.shared.secure.gcp.InstanceDocument;
import com.uid2.shared.secure.gcp.InstanceDocumentVerifier;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GcpVmidAttestationProvider implements IAttestationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpVmidAttestationProvider.class);

    private final InstanceDocumentVerifier idVerifier = new InstanceDocumentVerifier();
    private final VmConfigVerifier vmConfigVerifier;
    private final Set<String> allowedVmConfigIds = new HashSet<>();

    public GcpVmidAttestationProvider(GoogleCredentials credentials, Set<String> enclaveParams) throws Exception {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
        this.vmConfigVerifier = new VmConfigVerifier(credentials, enclaveParams);
        LOGGER.info("Using Google Service Account: " + credentials.toString());
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        // check instance document
        final InstanceDocument vmid;
        try {
            String request = new String(attestationRequest, StandardCharsets.US_ASCII);
            vmid = idVerifier.verify(request);
        }
        catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
            return;
        }

        LOGGER.debug("Validating Instance Confidentiality...");
        if (!vmid.getInstanceConfidentiality()) {
            // return attestation failure for non-confidential-vm
            handler.handle(Future.failedFuture(new AttestationException("not on confidential vm")));
            return;

        }

        LOGGER.debug("Validating client public key...");
        // check client public key matches audience in instance document
        try {
            byte[] signedPubKey = Utils.decodeBase64String(vmid.getAudience());
            if (!Arrays.equals(signedPubKey, publicKey)) {
                handler.handle(Future.failedFuture(new AttestationException("Invalid or mismatched client public key")));
                return;
            }
        }
        catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
            return;
        }

        // extract vmConfigId using information from instance document
        LOGGER.debug("Validating VmConfig...");
        final String vmConfigId;
        try {
            vmConfigId = vmConfigVerifier.getVmConfigId(vmid);
        }
        catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
            return;
        }

        // check if vmConfigId is approved/allowed
        if (vmConfigId == null) {
            handler.handle(Future.failedFuture(new AttestationException("Invalid or null vmConfigId")));
            return;
        }

        LOGGER.debug("VmConfigId = " + vmConfigId + ", validating against " +  allowedVmConfigIds.size() + " registered enclaves");
        if (VmConfigVerifier.VALIDATE_VMCONFIG && !allowedVmConfigIds.contains(vmConfigId)) {
            handler.handle(Future.failedFuture(new AttestationException("Invalid or null vmConfigId")));
            return;
        } else if (!VmConfigVerifier.VALIDATE_VMCONFIG) {
            LOGGER.fatal("Skip VmConfig validation (VALIDATE_VMCONFIG off)...");
        }

        LOGGER.debug("Successfully attested VmConfigId against registered enclaves");

        // return successful attestation with public key if all above checks pass
        AttestationResult result = new AttestationResult(publicKey);
        handler.handle(Future.succeededFuture(result));
    }

    @Override
    public void registerEnclave(String vmConfigId) throws AttestationException {
        try {
            allowedVmConfigIds.add(vmConfigId);
        } catch (Exception e) {
            LOGGER.error(e);
            throw new AttestationException(e);
        }
    }

    @Override
    public void unregisterEnclave(String vmConfigId) throws AttestationException {
        try {
            allowedVmConfigIds.remove(vmConfigId);
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public Collection<String> getEnclaveAllowlist() {
        return allowedVmConfigIds;
    }
}
