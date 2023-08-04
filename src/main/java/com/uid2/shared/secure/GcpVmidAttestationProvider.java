package com.uid2.shared.secure;

import com.google.auth.oauth2.GoogleCredentials;
import com.uid2.shared.Utils;
import com.uid2.shared.secure.gcp.VmConfigId;
import com.uid2.shared.secure.gcp.VmConfigVerifier;
import com.uid2.shared.secure.gcp.InstanceDocument;
import com.uid2.shared.secure.gcp.InstanceDocumentVerifier;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final VmConfigId vmConfigId;
        try {
            vmConfigId = vmConfigVerifier.getVmConfigId(vmid);
        }
        catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
            return;
        }

        // check if vmConfigId is approved/allowed
        if (!vmConfigId.isValid()) {
            final String errorMessage = vmConfigId.getProjectId() == null ?
                    vmConfigId.getFailedReason() :
                    vmConfigId.getProjectId() + " @ " + vmConfigId.getFailedReason();
            handler.handle(Future.failedFuture(new AttestationException(errorMessage)));
            return;
        }

        LOGGER.debug("VmConfigId = " + vmConfigId + ", validating against " + allowedVmConfigIds.size() + " registered enclaves");
        if (VmConfigVerifier.VALIDATE_VMCONFIG && !allowedVmConfigIds.contains(vmConfigId.getValue())) {
            handler.handle(Future.failedFuture(new AttestationException("unauthorized vmConfigId")));
            return;
        } else if (!VmConfigVerifier.VALIDATE_VMCONFIG) {
            LOGGER.error("Skip VmConfig validation (VALIDATE_VMCONFIG off)...");
        }

        LOGGER.debug("Successfully attested VmConfigId against registered enclaves");

        // return successful attestation with public key if all above checks pass
        AttestationResult result = new AttestationResult(publicKey, vmConfigId.getValue());
        handler.handle(Future.succeededFuture(result));
    }

    @Override
    public void registerEnclave(String vmConfigId) throws AttestationException {
        try {
            allowedVmConfigIds.add(vmConfigId);
        } catch (Exception e) {
            LOGGER.error("registerEnclave", e);
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
