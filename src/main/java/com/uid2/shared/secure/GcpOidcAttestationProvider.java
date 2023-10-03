package com.uid2.shared.secure;

import com.uid2.shared.secure.gcpoidc.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GcpOidcAttestationProvider implements IAttestationProvider{
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpOidcAttestationProvider.class);

    private final ITokenSignatureValidator tokenSignatureValidator;

    private final List<IPolicyValidator> supportedPolicyValidators;

    private final Set<String> allowedEnclaveIds = new HashSet<>();

    public GcpOidcAttestationProvider(){
        this(new TokenSignatureValidator(), Arrays.asList(new PolicyValidator()));
    }

    // used in UT
    protected GcpOidcAttestationProvider(ITokenSignatureValidator tokenSignatureValidator, List<IPolicyValidator> supportedPolicyValidators){
        this.tokenSignatureValidator = tokenSignatureValidator;
        this.supportedPolicyValidators = supportedPolicyValidators;
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        try {
            var tokenString = new String(attestationRequest, StandardCharsets.US_ASCII);

            LOGGER.debug("Validating signature...");
            var tokenPayload = tokenSignatureValidator.validate(tokenString);

            var enclaveId = this.validate(tokenPayload);
            if (enclaveId != null) {
                handler.handle(Future.succeededFuture(new AttestationResult(publicKey, enclaveId)));
            } else {
                throw new AttestationException("unauthorized token");
            }
        }
        catch (AttestationException ex){
            handler.handle(Future.failedFuture(ex));
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

    // Pass as long as one of supported policy validator check pass.
    // Returns
    //     null if validation failed
    //     enclaveId if validation succeed
    private String validate(TokenPayload tokenPayload) {
        for (var policyValidator : supportedPolicyValidators) {
            LOGGER.info("Validating policy... Validator version: " + policyValidator.getVersion());
            try {
                var enclaveId = policyValidator.validate(tokenPayload);
                LOGGER.info("Validator version: " + policyValidator.getVersion() + ", result: " + enclaveId);

                if (allowedEnclaveIds.contains(enclaveId)) {
                    LOGGER.info("Successfully attested OIDC against registered enclaves");
                    return enclaveId;
                }
            } catch (Exception ex) {
                LOGGER.warn("Fail to validator version: " + policyValidator.getVersion() + ", error :" + ex.getMessage());
            }
        }
        return null;
    }
}
