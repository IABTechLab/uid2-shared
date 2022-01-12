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

    public static JsonObject UnauthorizedResponse = new JsonObject(new HashMap<String, Object>() {
        {
            put("status", Const.ResponseStatus.Unauthorized);
        }
    });
    private static final String AuthorizationHeader = "Authorization";
    private static final String PrefixString = "bearer "; // The space at the end is intentional
    private IAuthorizableProvider authKeyStore;
    private IAuthorizable internalClient;

    private static final IAuthorizationProvider blankAuthorizationProvider = new BlankAuthorizationProvider();

    public AuthMiddleware(IAuthorizableProvider authKeyStore) {
        this.authKeyStore = authKeyStore;
    }

    public void setInternalClientKey(IAuthorizable internalClient) {
        if (this.internalClient != null) throw new IllegalStateException("internalClient already set");
        this.internalClient = internalClient;
    }

    public static String getAuthToken(RoutingContext rc) {
        return AuthHandler.extractBearerToken(rc.request().getHeader(AuthorizationHeader));
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

    private IAuthorizable getAuthClientByKey(String key) {
        return this.authKeyStore.get(key);
    }

    public <E> Handler<RoutingContext> handleV1(Handler<RoutingContext> handler, E... roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("must specify at least one role");
        }
        final RoleBasedAuthorizationProvider<E> authorizationProvider = new RoleBasedAuthorizationProvider<>(Collections.unmodifiableSet(new HashSet<E>(Arrays.asList(roles))));
        final AuthHandler h = new AuthHandler(handler, this.authKeyStore, authorizationProvider, true);
        return h::handle;
    }

    public <E> Handler<RoutingContext> handle(Handler<RoutingContext> handler, E... roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("must specify at least one role");
        }
        final RoleBasedAuthorizationProvider<E> authorizationProvider = new RoleBasedAuthorizationProvider<>(Collections.unmodifiableSet(new HashSet<E>(Arrays.asList(roles))));
        final AuthHandler h = new AuthHandler(handler, this.authKeyStore, authorizationProvider, false);
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

    public Handler<RoutingContext> internalOnly(Handler<RoutingContext> handler) {
        final ServiceInternalHandler h = new ServiceInternalHandler(handler, this.internalClient);
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
                AuthMiddleware.setAuthClient(rc, internalClient);
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
