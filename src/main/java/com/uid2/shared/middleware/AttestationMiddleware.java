package com.uid2.shared.middleware;

import com.uid2.shared.attest.IAttestationTokenService;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class AttestationMiddleware {

    private final IAttestationTokenService tokenService;

    public AttestationMiddleware(IAttestationTokenService tokenService) {
        this.tokenService = tokenService;
    }

    //region RequestHandler

    public static final String AttestationTokenHeader = "Attestation-Token";
    public static final String AttestationJWTHeader = "Attestation-JWT";

    public Handler<RoutingContext> handle(Handler<RoutingContext> handler) {
        final AttestationHandler wrapper = new AttestationHandler(handler, this.tokenService);
        return wrapper::handle;
    }

    private static class AttestationHandler {

        private final Handler<RoutingContext> next;
        private final IAttestationTokenService attestor;

        AttestationHandler(Handler<RoutingContext> next, IAttestationTokenService attestor) {
            this.next = next;
            this.attestor = attestor;
        }

        public void handle(RoutingContext rc) {
            boolean success = false;

            final IAuthorizable profile = AuthMiddleware.getAuthClient(rc);
            if (profile instanceof OperatorKey) {
                final String protocol = ((OperatorKey) profile).getProtocol();
                final String userToken = AuthMiddleware.getAuthToken(rc);
                final String jwt = getAttestationJWT(rc);

                final String encryptedToken = getAttestationToken(rc);
                if ("trusted".equals(protocol)) {
                    // (pre-)trusted operator requires no-attestation
                    success = true;
                } else if (encryptedToken != null && userToken != null) {
                    success = attestor.validateToken(userToken, encryptedToken);
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
