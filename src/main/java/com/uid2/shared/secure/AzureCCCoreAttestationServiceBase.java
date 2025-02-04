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
public abstract class AzureCCCoreAttestationServiceBase implements ICoreAttestationService {

    protected final Set<String> allowedEnclaveIds = new HashSet<>();

    protected final IMaaTokenSignatureValidator tokenSignatureValidator;

    protected final IPolicyValidator policyValidator;

    protected final String azureCcProtocol;

    // used in UT
    protected AzureCCCoreAttestationServiceBase(IMaaTokenSignatureValidator tokenSignatureValidator, IPolicyValidator policyValidator, String azureCcProtocol) {
        this.tokenSignatureValidator = tokenSignatureValidator;
        this.policyValidator = policyValidator;
        this.azureCcProtocol = azureCcProtocol;
    }

    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        try {
            var tokenString = new String(attestationRequest, StandardCharsets.US_ASCII);

            log.debug("Validating signature...");
            var tokenPayload = tokenSignatureValidator.validate(tokenString, azureCcProtocol);

            log.debug("Validating policy...");
            var encodedPublicKey = Utils.toBase64String(publicKey);

            var enclaveId = policyValidator.validate(tokenPayload, encodedPublicKey);

            if (allowedEnclaveIds.contains(enclaveId)) {
                log.info("Successfully attested " + azureCcProtocol + " against registered enclaves, enclave id: " + enclaveId);
                handler.handle(Future.succeededFuture(new AttestationResult(publicKey, enclaveId)));
            } else {
                log.warn("Got unsupported " + azureCcProtocol + " enclave id: " + enclaveId);
                handler.handle(Future.succeededFuture(new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE)));
            }
        }
        catch (AttestationClientException ace){
            handler.handle(Future.succeededFuture(new AttestationResult(ace)));
        } catch (AttestationException ae) {
            handler.handle(Future.failedFuture(ae));
        } catch (Exception ex) {
            handler.handle(Future.failedFuture(new AttestationException(ex)));
        }
    };

    public void registerEnclave(String encodedIdentifier) throws AttestationException {
        try {
            allowedEnclaveIds.add(encodedIdentifier);
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    public void unregisterEnclave(String encodedIdentifier) throws AttestationException {
        try {
            allowedEnclaveIds.remove(encodedIdentifier);
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    public Collection<String> getEnclaveAllowlist() {
        return allowedEnclaveIds;
    }
}
