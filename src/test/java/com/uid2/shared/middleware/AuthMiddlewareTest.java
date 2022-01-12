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

import com.uid2.shared.auth.IAuthorizableProvider;
import com.uid2.shared.auth.IRoleAuthorizable;
import com.uid2.shared.auth.Role;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthMiddlewareTest {
    @Mock private IAuthorizableProvider authProvider;
    @Mock private RoutingContext routingContext;
    @Mock private HttpServerRequest request;
    @Mock private Handler<RoutingContext> nextHandler;
    @Mock private IRoleAuthorizable<Role> profile;
    private AuthMiddleware auth;

    @Before public void setup() {
        auth = new AuthMiddleware(authProvider);
        when(routingContext.request()).thenReturn(request);
    }

    @Test public void authHandlerNoAuthorizationHeader() {
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
    }

    @Test public void authHandlerInvalidAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bogus Header Value");
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
    }

    @Test public void authHandlerUnknownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer unknown-key");
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
    }

    @Test public void authHandlerKeyWithoutRoles() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
    }

    @Test public void authHandlerKeyWithFirstRole() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.MAPPER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }

    @Test public void authHandlerKeyWithSecondRole() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.ID_READER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }

    @Test public void authHandlerKeyDisabled() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.isDisabled()).thenReturn(true);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verify(profile, times(0)).hasRole(any());
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
    }

    @Test public void noAuthHandlerNoAuthorizationHeader() {
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }

    @Test public void noAuthHandlerInvalidAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bogus Header Value");
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }

    @Test public void noAuthHandlerUnknownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }

    @Test public void noAuthHandlerKnownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(profile, times(0)).isDisabled();
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
    }
}
