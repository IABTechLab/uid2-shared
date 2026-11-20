package com.uid2.shared.secure;

import com.uid2.shared.secure.gcpoidc.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GcpOidcCoreAttestationService implements ICoreAttestationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpOidcCoreAttestationService.class);

    private final ITokenSignatureValidator tokenSignatureValidator;

    private final List<IPolicyValidator> supportedPolicyValidators;

    private final Set<String> allowedEnclaveIds = new HashSet<>();

    public GcpOidcCoreAttestationService(String attestationUrl){
        this(new TokenSignatureValidator(), Arrays.asList(new PolicyValidator(attestationUrl)));
    }

    // used in UT
    protected GcpOidcCoreAttestationService(ITokenSignatureValidator tokenSignatureValidator, List<IPolicyValidator> supportedPolicyValidators){
        this.tokenSignatureValidator = tokenSignatureValidator;
        this.supportedPolicyValidators = supportedPolicyValidators;
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        TokenPayload tokenPayload = null;
        try {
            var tokenString = new String(attestationRequest, StandardCharsets.US_ASCII);

            LOGGER.debug("Validating signature...");
            tokenPayload = tokenSignatureValidator.validate(tokenString);

            var enclaveId = this.validate(tokenPayload);
            if (enclaveId != null) {
                LOGGER.info("Successfully attested gcp-oidc against registered enclaves, enclave id: " + enclaveId);
                handler.handle(Future.succeededFuture(new AttestationResult(publicKey, enclaveId).withDetails(getAttestationDetails(tokenPayload))));
            } else {
                LOGGER.warn("Can not find registered gcp-oidc enclave id.");
                handler.handle(Future.succeededFuture(new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE).withDetails(getAttestationDetails(tokenPayload))));
            }
        }
        catch (AttestationClientException ace){
            handler.handle(Future.succeededFuture(new AttestationResult(ace).withDetails(getAttestationDetails(tokenPayload))));
        }
        catch (AttestationException ae){
            handler.handle(Future.failedFuture(ae));
        }
        catch (Exception ex) {
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

    private static Map<String, String> getAttestationDetails(TokenPayload tokenPayload) {
        var details = new LinkedHashMap<String, String>();
        if (tokenPayload != null && tokenPayload.getEnvOverrides() != null) {
            for (var key : PolicyValidator.REPORTED_ENV_OVERRIDES) {
                var value = tokenPayload.getEnvOverrides().get(key);
                if (value != null) {
                    details.put(key, value);
                }
            }
        }
        return details;
    }

    // Pass as long as one of supported policy validator check pass.
    // Returns
    //     null if validation failed
    //     enclaveId if validation succeed
    private String validate(TokenPayload tokenPayload) throws Exception {
        Exception lastException = null;
        for (var policyValidator : supportedPolicyValidators) {
            LOGGER.info("Validating policy... Validator version: " + policyValidator.getVersion());
            try {
                var enclaveId = policyValidator.validate(tokenPayload);
                LOGGER.info("Validator version: " + policyValidator.getVersion() + ", result: " + enclaveId);

                if (allowedEnclaveIds.contains(enclaveId)) {
                    LOGGER.info("Successfully attested gcp-oidc against registered enclaves");
                    return enclaveId;
                } else {
                    LOGGER.warn("Got unsupported gcp-oidc enclave id: " + enclaveId);
                }
            } catch (Exception ex) {
                lastException = ex;
                LOGGER.warn("Fail to validator version: " + policyValidator.getVersion() + ", error :" + ex.getMessage());
            }
        }

        if(lastException != null){
            throw lastException;
        }
        return null;
    }
}
