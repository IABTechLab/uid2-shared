package com.uid2.shared.middleware;

import com.uid2.shared.Const;
import com.uid2.shared.audit.Audit;
import com.uid2.shared.audit.AuditParams;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.*;

public class AuthMiddleware {
    public static final String API_CONTACT_PROP = "api-contact";
    public static final String API_CLIENT_PROP = "api-client";

    public static final JsonObject UnauthorizedResponse = new JsonObject(new HashMap<String, Object>() {
        {
            put("status", Const.ResponseStatus.Unauthorized);
        }
    });
    private static final String AuthorizationHeader = "Authorization";
    private static final String PrefixString = "bearer "; // The space at the end is intentional
    private IAuthorizableProvider authKeyStore;
    private final Audit audit;

    private static final IAuthorizationProvider blankAuthorizationProvider = new BlankAuthorizationProvider();

    public AuthMiddleware(IAuthorizableProvider authKeyStore) {
        this(authKeyStore, "unknown");
    }

    public AuthMiddleware(IAuthorizableProvider authKeyStore, String auditSource) {
        this.authKeyStore = authKeyStore;
        this.audit = new Audit(auditSource);
    }

    public static String getAuthToken(RoutingContext rc) {
        return AuthHandler.extractBearerToken(rc.request().getHeader(AuthorizationHeader));
    }

    public static boolean isAuthenticated(RoutingContext rc) {
        return rc.data().get(API_CLIENT_PROP) != null;
    }

    public static IAuthorizable getAuthClient(RoutingContext rc) {
        return (IAuthorizable) rc.data().get(API_CLIENT_PROP);
    }

    public static <U extends IAuthorizable> U getAuthClient(Class<U> type, RoutingContext rc) {
        return (U) rc.data().get(API_CLIENT_PROP);
    }

    public static void setAuthClient(RoutingContext rc, IAuthorizable profile) {
        rc.data().put(API_CLIENT_PROP, profile);
        if (profile != null) {
            rc.data().put(API_CONTACT_PROP, profile.getContact());
        }
    }

    public <E> Handler<RoutingContext> handleV1(Handler<RoutingContext> handler, E... roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("must specify at least one role");
        }
        final RoleBasedAuthorizationProvider<E> authorizationProvider = new RoleBasedAuthorizationProvider<>(Collections.unmodifiableSet(new HashSet<E>(Arrays.asList(roles))));
        final AuthHandler h = new AuthHandler(handler, this.authKeyStore, authorizationProvider, true);
        return h::handle;
    }

    private Handler<RoutingContext> logAndHandle(Handler<RoutingContext> handler, AuditParams params) {
        return ctx -> {
            ctx.addBodyEndHandler(v -> this.audit.log(ctx, params));
            handler.handle(ctx);
        };
    }

    public <E> Handler<RoutingContext> handle(Handler<RoutingContext> handler, E... roles) {
        return this.handleWithAudit(handler, null, false, roles);
    }

    public <E> Handler<RoutingContext> handleWithAudit(Handler<RoutingContext> handler, AuditParams params, boolean enableAuditLog, E... roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("must specify at least one role");
        }
        final RoleBasedAuthorizationProvider<E> authorizationProvider = new RoleBasedAuthorizationProvider<>(Collections.unmodifiableSet(new HashSet<E>(Arrays.asList(roles))));
        AuthHandler h;
        if (enableAuditLog) {
            final Handler<RoutingContext> loggedHandler = logAndHandle(handler, params);
            h = new AuthHandler(loggedHandler, this.authKeyStore, authorizationProvider, false);
        } else {
            h = new AuthHandler(handler, this.authKeyStore, authorizationProvider, false);
        }

        return h::handle;
    }


    public Handler<RoutingContext> handleWithOptionalAuth(Handler<RoutingContext> handler) {
        final AuthHandler h = new AuthHandler(handler, this.authKeyStore, blankAuthorizationProvider, true);
        return h::handle;
    }

    public Handler<RoutingContext> loopbackOnly(Handler<RoutingContext> handler, IAuthorizable clientKey) {
        final LoopbackOnlyHandler h = new LoopbackOnlyHandler(handler, clientKey);
        return h::handle;
    }

    private static class BlankAuthorizationProvider implements IAuthorizationProvider {
        @Override
        public boolean isAuthorized(IAuthorizable profile) {
            return true;
        }
    }

    private static class LoopbackOnlyHandler {
        private final Handler<RoutingContext> innerHandler;
        private final IAuthorizable clientKey;

        private LoopbackOnlyHandler(Handler<RoutingContext> handler, IAuthorizable clientKey) {
            this.innerHandler = handler;
            this.clientKey = clientKey;
        }

        public void handle(RoutingContext rc) {
            String host = rc.request().host();
            if (host == null || !host.startsWith("127.0.0.1")) {
                // Host not specified, or Host not start with 127.0.0.1
                rc.fail(401);
            } else {
                AuthMiddleware.setAuthClient(rc, clientKey);
                this.innerHandler.handle(rc);
            }
        }
    }

    private static class AuthHandler {
        private final Handler<RoutingContext> innerHandler;
        private final IAuthorizableProvider authKeyStore;
        private final IAuthorizationProvider authorizationProvider;
        private final boolean isV1Response;

        private AuthHandler(Handler<RoutingContext> handler, IAuthorizableProvider authKeyStore, IAuthorizationProvider authorizationProvider, boolean isV1Response) {
            this.innerHandler = handler;
            this.authKeyStore = authKeyStore;
            this.authorizationProvider = authorizationProvider;
            this.isV1Response = isV1Response;
        }

        public void handle(RoutingContext rc) {
            // add aws request id tracer to help validation
            String traceId = rc.request().getHeader("X-Amzn-Trace-Id");
            if (traceId != null && traceId.length() > 0) {
                rc.response().headers().add("X-Amzn-Trace-Id", traceId);
            }

            final String authHeaderValue = rc.request().getHeader(AuthMiddleware.AuthorizationHeader);
            final String authKey = AuthHandler.extractBearerToken(authHeaderValue);
            final IAuthorizable profile = this.authKeyStore.get(authKey);
            AuthMiddleware.setAuthClient(rc, profile);
            if (this.authorizationProvider.isAuthorized(profile)) {
                this.innerHandler.handle(rc);
            } else {
                this.onFailedAuth(rc);
            }
        }

        private void onFailedAuth(RoutingContext rc) {
            if (isV1Response) {
                rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setStatusCode(401)
                        .end(UnauthorizedResponse.encode());
            }
            rc.fail(401);
        }

        private static String extractBearerToken(final String headerValue) {
            if (headerValue == null) {
                return null;
            }

            final String v = headerValue.trim();
            if (v.length() < PrefixString.length()) {
                return null;
            }

            final String givenPrefix = v.substring(0, PrefixString.length());

            if (!PrefixString.equals(givenPrefix.toLowerCase())) {
                return null;
            }
            return v.substring(PrefixString.length());
        }
    }

}
