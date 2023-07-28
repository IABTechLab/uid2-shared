package com.uid2.shared.middleware;

import com.uid2.shared.attest.IAttestationTokenService;
import com.uid2.shared.attest.JwtService;
import com.uid2.shared.attest.JwtValidationResponse;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttestationMiddleware {

    private final IAttestationTokenService tokenService;
    private final JwtService jwtService;
    private final String jwtAudience;
    private final String jwtIssuer;

    public AttestationMiddleware(IAttestationTokenService tokenService, JwtService jwtService, String jwtAudience, String jwtIssuer) {
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.jwtAudience = jwtAudience;
        this.jwtIssuer = jwtIssuer;
    }

    //region RequestHandler

    public static final String AttestationTokenHeader = "Attestation-Token";
    public static final String AttestationJWTHeader = "Attestation-JWT";

    public Handler<RoutingContext> handle(Handler<RoutingContext> handler) {
        final AttestationHandler wrapper = new AttestationHandler(handler, this.tokenService, this.jwtService, this.jwtAudience, this.jwtIssuer);
        return wrapper::handle;
    }

    private static class AttestationHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(AttestationHandler.class);
        private final Handler<RoutingContext> next;
        private final IAttestationTokenService attestor;
        private final JwtService jwtService;
        private final String jwtAudience;
        private final String jwtIssuer;

        AttestationHandler(Handler<RoutingContext> next, IAttestationTokenService attestor, JwtService jwtService, String jwtAudience, String jwtIssuer) {
            this.next = next;
            this.attestor = attestor;
            this.jwtService = jwtService;
            this.jwtAudience = jwtAudience;
            this.jwtIssuer = jwtIssuer;
        }

        public void handle(RoutingContext rc) {
            boolean success = false;

            final IAuthorizable profile = AuthMiddleware.getAuthClient(rc);
            if (profile instanceof OperatorKey) {
                OperatorKey operatorKey = (OperatorKey)profile;
                final String protocol = operatorKey.getProtocol();
                final String userToken = AuthMiddleware.getAuthToken(rc);
                final String jwt = getAttestationJWT(rc);

                final String encryptedToken = getAttestationToken(rc);
                if ("trusted".equals(protocol)) {
                    // (pre-)trusted operator requires no-attestation
                    success = true;
                } else if (encryptedToken != null && userToken != null) {
                    success = attestor.validateToken(userToken, encryptedToken);
                }

                if (success) {
                    if ((jwt != null && !jwt.isEmpty())) {
                        try {
                            JwtValidationResponse response = jwtService.validateJwt(jwt, this.jwtAudience, this.jwtIssuer);
                            success = response.getIsValid();
                        } catch (JwtService.ValidationException e) {
                            LOGGER.error("Error validating JWT. Attestation failed.", e);
                            success = false;
                        }
                    }
                }
            }

            if (success) {
                next.handle(rc);
            } else {
                onFailedAttestation(rc);
            }
        }

        private void onFailedAttestation(RoutingContext rc) {
            rc.fail(401);
        }

        private String getAttestationToken(RoutingContext rc) {
            return rc.request().getHeader(AttestationTokenHeader);
        }

        private String getAttestationJWT(RoutingContext rc) {
            return rc.request().getHeader(AttestationJWTHeader);
        }
    }

    //endregion RequestHandler
}
