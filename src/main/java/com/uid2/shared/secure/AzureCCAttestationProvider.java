package com.uid2.shared.secure;

import com.uid2.shared.Utils;
import com.uid2.shared.secure.azurecc.IMaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.IPolicyValidator;
import com.uid2.shared.secure.azurecc.MaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.PolicyValidator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// CC stands for Confidential Container
@Slf4j
public class AzureCCAttestationProvider implements IAttestationProvider {

    private final Set<String> allowedEnclaveIds = new HashSet<>();

    private final IMaaTokenSignatureValidator tokenSignatureValidator;

    private final IPolicyValidator policyValidator;

    public AzureCCAttestationProvider(String maaServerBaseUrl) {
        this(new MaaTokenSignatureValidator(maaServerBaseUrl), new PolicyValidator());
    }

    // used in UT
    protected AzureCCAttestationProvider(IMaaTokenSignatureValidator tokenSignatureValidator, IPolicyValidator policyValidator) {
        this.tokenSignatureValidator = tokenSignatureValidator;
        this.policyValidator = policyValidator;
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        try {
            var tokenString = new String(attestationRequest, StandardCharsets.US_ASCII);

            log.debug("Validating signature...");
            var tokenPayload = tokenSignatureValidator.validate(tokenString);

            log.debug("Validating policy...");
            var encodedPublicKey = Utils.toBase64String(publicKey);

            var enclaveId = policyValidator.validate(tokenPayload, encodedPublicKey);

            if (allowedEnclaveIds.contains(enclaveId)) {
                log.info("Successfully attested azure-cc against registered enclaves, enclave id: " + enclaveId);
                handler.handle(Future.succeededFuture(new AttestationResult(publicKey, enclaveId)));
            } else {
                throw new AttestationException("Unregistered enclave, enclave id: " + enclaveId);
            }
        } catch (AttestationException ex) {
            handler.handle(Future.failedFuture(ex));
        } catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
        }
    }

    @Override
    public void registerEnclave(String encodedIdentifier) throws AttestationException {
        try {
            allowedEnclaveIds.add(encodedIdentifier);
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public void unregisterEnclave(String encodedIdentifier) throws AttestationException {
        try {
            allowedEnclaveIds.remove(encodedIdentifier);
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public Collection<String> getEnclaveAllowlist() {
        return allowedEnclaveIds;
    }
}