package com.uid2.shared.middleware;

import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.attest.IAttestationTokenService;
import com.uid2.shared.attest.JwtService;
import com.uid2.shared.attest.JwtValidationResponse;
import com.uid2.shared.attest.RoleBasedJwtClaimValidator;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static com.uid2.shared.Utils.createMessageDigestSHA512;

public class AttestationMiddleware {

    private final IAttestationTokenService tokenService;
    private final JwtService jwtService;
    private final String jwtAudience;
    private final String jwtIssuer;
    private final boolean enforceJwt;

    public AttestationMiddleware(IAttestationTokenService tokenService, JwtService jwtService, String jwtAudience, String jwtIssuer, boolean enforceJwt) {
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.jwtAudience = jwtAudience;
        this.jwtIssuer = jwtIssuer;
        this.enforceJwt = enforceJwt;
    }

    //region RequestHandler

    public Handler<RoutingContext> handle(Handler<RoutingContext> handler, com.uid2.shared.auth.Role... roles) {
        final RoleBasedJwtClaimValidator validator = new RoleBasedJwtClaimValidator(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles))));
        final AttestationHandler wrapper = new AttestationHandler(handler, this.tokenService, this.jwtService, this.jwtAudience, this.jwtIssuer, this.enforceJwt, validator);
        return wrapper::handle;
    }

    private static class AttestationHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(AttestationHandler.class);
        private final Handler<RoutingContext> next;
        private final IAttestationTokenService attestor;
        private final JwtService jwtService;
        private final String jwtAudience;
        private final String jwtIssuer;
        private final boolean enforceJwt;
        private final RoleBasedJwtClaimValidator roleBasedJwtClaimValidator;

        AttestationHandler(Handler<RoutingContext> next, IAttestationTokenService attestor, JwtService jwtService, String jwtAudience, String jwtIssuer, boolean enforceJwt, RoleBasedJwtClaimValidator roleBasedJwtClaimValidator) {
            this.next = next;
            this.attestor = attestor;
            this.jwtService = jwtService;
            this.jwtAudience = jwtAudience;
            this.jwtIssuer = jwtIssuer;
            this.enforceJwt = enforceJwt;
            this.roleBasedJwtClaimValidator = roleBasedJwtClaimValidator;
        }

        public void handle(RoutingContext rc) {
            boolean success = false;
            boolean isJwtValid = false;

            final IAuthorizable profile = AuthMiddleware.getAuthClient(rc);
            if (profile instanceof OperatorKey) {
                OperatorKey operatorKey = (OperatorKey) profile;
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
                    if (jwt != null && !jwt.isBlank()) {
                        try {
                            JwtValidationResponse response = jwtService.validateJwt(jwt, this.jwtAudience, this.jwtIssuer);
                            isJwtValid = response.getIsValid();
                            if (isJwtValid) {
                                if (!this.roleBasedJwtClaimValidator.hasRequiredRoles(response)) {
                                    isJwtValid = false;
                                    LOGGER.info("JWT missing required role. Required roles: {}, JWT Presented Roles: {}, SiteId: {}, Name: {}, Contact: {}", this.roleBasedJwtClaimValidator.getRequiredRoles(), response.getRoles(), operatorKey.getSiteId(), operatorKey.getName(), operatorKey.getContact());
                                }

                                String subject = calculateSubject(operatorKey);
                                if (!validateSubject(response, subject)) {
                                    isJwtValid = false;
                                    LOGGER.info("JWT failed validation of Subject. JWT Presented Roles: {}, SiteId: {}, Name: {}, Contact: {}, JWT Subject: {}, Operator Subject: {}", response.getRoles(), operatorKey.getSiteId(), operatorKey.getName(), operatorKey.getContact(), response.getSubject(), subject);
                                }
                            }
                        } catch (JwtService.ValidationException e) {
                            LOGGER.info("Error validating JWT. Attestation validation failed. SiteId: {}, Name: {}, Contact: {}. Error: {}", operatorKey.getSiteId(), operatorKey.getName(), operatorKey.getContact(), e.toString());
                        }
                    } else {
                        if (this.enforceJwt) {
                            LOGGER.info("JWT is required, but was not received. Attestation validation failed. SiteId: {}, Name: {}, Contact: {}", operatorKey.getSiteId(), operatorKey.getName(), operatorKey.getContact());
                        } else {
                            LOGGER.info("JWT is {} but is not required. SiteId: {}, Name: {}, Url: {}", jwt == null ? "null" : "blank", operatorKey.getSiteId(), operatorKey.getName(), rc.request().uri());
                        }
                    }
                }
            }

            if (success && !isJwtValid && this.enforceJwt) {
                LOGGER.error("JWT validation has failed.");
                success = false;
            } else if (success && !isJwtValid && !this.enforceJwt) {
                LOGGER.info("JWT validation has failed, but JWTs are not being enforced.");
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
            return rc.request().getHeader(Const.Attestation.AttestationTokenHeader);
        }

        private String getAttestationJWT(RoutingContext rc) {
            return rc.request().getHeader(Const.Attestation.AttestationJWTHeader);
        }

        private static String calculateSubject(OperatorKey operatorKey) {
            if (operatorKey.getKeyHash() == null || operatorKey.getKeyHash().isBlank()) {
                return "";
            }

            byte[] keyBytes = operatorKey.getKeyHash().getBytes();
            MessageDigest md = createMessageDigestSHA512();
            byte[] hashBytes = md.digest(keyBytes);
            return Utils.toBase64String(hashBytes);
        }

        private static Boolean validateSubject(JwtValidationResponse response, String subject) {
            if (response.getSubject() == null || response.getSubject().isBlank()) {
                return false;
            }

            return subject.equals(response.getSubject());
        }
    }

    //endregion RequestHandler
}
