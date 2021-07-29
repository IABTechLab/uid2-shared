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

package com.uid2.shared.middleware;

import com.uid2.shared.Const;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.*;

public class AuthMiddleware {
    public static final String API_CONTACT_PROP = "api-contact";
    public static final String API_CLIENT_PROP = "api-client";

    public static JsonObject UnanthorizedResponse = new JsonObject(new HashMap<String, Object>() {
        {
            put("status", Const.ResponseStatus.Unauthorized);
        }
    });
    private static String AuthorizationHeader = "Authorization";
    private static String PrefixString = "bearer "; // The space at the end is intentional
    private IAuthProvider authKeyStore;
    private IAuthorizable internalClient;

    public AuthMiddleware(IAuthProvider authKeyStore) {
        this.authKeyStore = authKeyStore;
    }

    public void setInternalClientKey(String key) {
        if (internalClient != null) throw new IllegalStateException("internalClient already set");
        this.internalClient = new ClientKey(key)
            .withNameAndContact("service-internal");
    }

    public static String getAuthToken(RoutingContext rc) {
        return AuthHandler.extractBearerToken(rc.request().getHeader(AuthorizationHeader));
    }

    public static IAuthorizable getAuthClient(RoutingContext rc) {
        return (IAuthorizable) rc.data().get(API_CLIENT_PROP);
    }

    private IAuthorizable getAuthKey(String key) {
        return this.authKeyStore.get(key);
    }

    public Handler<RoutingContext> handleV1(Handler<RoutingContext> handler, Role... roles) {
        if (roles == null || roles.length == 0) {
            return handler;
        }
        final AuthHandler h = new AuthHandler(handler, Collections.unmodifiableSet(new HashSet<Role>(Arrays.asList(roles))), this.authKeyStore, true);
        return h::handle;
    }

    public Handler<RoutingContext> handle(Handler<RoutingContext> handler, Role... roles) {
        if (roles == null || roles.length == 0) {
            return handler;
        }
        final AuthHandler h = new AuthHandler(handler, Collections.unmodifiableSet(new HashSet<Role>(Arrays.asList(roles))), this.authKeyStore);
        return h::handle;
    }

    public Handler<RoutingContext> handle(OptionalAuthHandler<RoutingContext> handler, Role... roles) {
        if (roles == null || roles.length == 0) {
            return new NoAuthHandler(handler)::handle;
        }
        final AuthHandler h = new AuthHandler(handler, Collections.unmodifiableSet(new HashSet<Role>(Arrays.asList(roles))), this.authKeyStore, true);
        return h::handle;
    }

    public Handler<RoutingContext> loopbackOnly(Handler<RoutingContext> handler) {
        final LoopbackOnlyHandler h = new LoopbackOnlyHandler(handler);
        return h::handle;
    }

    public Handler<RoutingContext> internalOnly(Handler<RoutingContext> handler) {
        final ServiceInternalHandler h = new ServiceInternalHandler(handler, this.internalClient);
        return h::handle;
    }

    private static class NoAuthHandler {
        private final OptionalAuthHandler<RoutingContext> innerHandler;

        private NoAuthHandler(OptionalAuthHandler<RoutingContext> handler) {
            this.innerHandler = handler;
        }

        public void handle(RoutingContext rc) {
            this.innerHandler.handle(rc, true);
        }

    }

    private static class LoopbackOnlyHandler {
        private final Handler<RoutingContext> innerHandler;
        private static ClientKey LOOPBACK_CLIENT = new ClientKey("no-key")
            .withNameAndContact("loopback");

        private LoopbackOnlyHandler(Handler<RoutingContext> handler) {
            this.innerHandler = handler;
        }

        public void handle(RoutingContext rc) {
            String host = rc.request().host();
            if (host == null || !host.startsWith("127.0.0.1")) {
                // Host not specified, or Host not start with 127.0.0.1
                rc.fail(401);
            } else {
                rc.data().put(API_CONTACT_PROP, LOOPBACK_CLIENT.getContact());
                rc.data().put(API_CLIENT_PROP, LOOPBACK_CLIENT);
                this.innerHandler.handle(rc);
            }
        }
    }

    private static class ServiceInternalHandler {
        private final Handler<RoutingContext> innerHandler;
        private final IAuthorizable internalClient;

        private ServiceInternalHandler(Handler<RoutingContext> handler, IAuthorizable internalClient) {
            this.innerHandler = handler;
            this.internalClient = internalClient;
        }

        public void handle(RoutingContext rc) {
            final String authHeaderValue = rc.request().getHeader(AuthMiddleware.AuthorizationHeader);
            final String authKey = AuthHandler.extractBearerToken(authHeaderValue);
            if (authKey == null || !authKey.equals(internalClient.getKey())) {
                // auth key doesn't match internal key
                rc.fail(401);
            } else {
                rc.data().put(API_CONTACT_PROP, internalClient.getContact());
                rc.data().put(API_CLIENT_PROP, internalClient);
                this.innerHandler.handle(rc);
            }
        }
    }

    private static class AuthHandler {
        private final Handler<RoutingContext> innerHandler;
        private final Set<Role> allowedRoles;
        private final IAuthProvider authKeyStore;
        private final OptionalAuthHandler<RoutingContext> optionalAuthHandler;
        private final boolean isV1Response;

        private AuthHandler(OptionalAuthHandler<RoutingContext> handler, Set<Role> allowedRoles, IAuthProvider authKeyStore) {
            this(handler, allowedRoles, authKeyStore, false);
        }

        private AuthHandler(Handler<RoutingContext> handler, Set<Role> allowedRoles, IAuthProvider authKeyStore) {
            this(handler, allowedRoles, authKeyStore, false);
        }

        private AuthHandler(OptionalAuthHandler<RoutingContext> handler, Set<Role> allowedRoles, IAuthProvider authKeyStore, boolean isV1Response) {
            this.innerHandler = null;
            this.allowedRoles = allowedRoles;
            this.authKeyStore = authKeyStore;
            this.optionalAuthHandler = handler;
            this.isV1Response = isV1Response;
        }

        private AuthHandler(Handler<RoutingContext> handler, Set<Role> allowedRoles, IAuthProvider authKeyStore, boolean isV1Response) {
            this.innerHandler = handler;
            this.allowedRoles = allowedRoles;
            this.authKeyStore = authKeyStore;
            this.optionalAuthHandler = null;
            this.isV1Response = isV1Response;
        }

        private boolean isOptional() {
            if (this.optionalAuthHandler == null) {
                return false;
            } else {
                return true;
            }
        }

        public void handle(RoutingContext rc) {
            // add aws request id tracer to help validation
            String traceId = rc.request().getHeader("X-Amzn-Trace-Id");
            if (traceId != null && traceId.length() > 0) {
                rc.response().headers().add("X-Amzn-Trace-Id", traceId);
            }

            if (this.allowedRoles == null || this.allowedRoles.isEmpty()) {
                this.onSuccessAuth(rc);
            } else {
                final String authHeaderValue = rc.request().getHeader(AuthMiddleware.AuthorizationHeader);
                final String authKey = AuthHandler.extractBearerToken(authHeaderValue);
                final IAuthorizable profile = this.authKeyStore.get(authKey);
                if (profile != null) {
                    rc.data().put(API_CLIENT_PROP, profile);
                    rc.data().put(API_CONTACT_PROP, profile.getContact());
                    if(this.allowedRoles.stream().anyMatch(profile::hasRole)) {
                        this.onSuccessAuth(rc);
                    } else {
                        this.onFailedAuth(rc);
                    }
                } else {
                    this.onFailedAuth(rc);
                }
            }
        }

        private void onSuccessAuth(RoutingContext rc) {
            if (this.isOptional()) {
                this.optionalAuthHandler.handle(rc, true);
            } else {
                this.innerHandler.handle(rc);
            }
        }

        private void onFailedAuth(RoutingContext rc) {
            if (this.isOptional()) {
                this.optionalAuthHandler.handle(rc, false);
            } else {
                if (isV1Response) {
                    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setStatusCode(401)
                        .end(UnanthorizedResponse.encode());
                }
                rc.fail(401);
            }
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
