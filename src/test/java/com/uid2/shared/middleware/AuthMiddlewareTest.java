package com.uid2.shared.middleware;

import com.uid2.shared.auth.IAuthorizableProvider;
import com.uid2.shared.auth.IRoleAuthorizable;
import com.uid2.shared.auth.Role;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthMiddlewareTest {
    @Mock
    private IAuthorizableProvider authProvider;
    @Mock
    private RoutingContext routingContext;
    @Mock
    private HttpServerRequest request;
    @Mock
    private Handler<RoutingContext> nextHandler;
    @Mock
    private IRoleAuthorizable<Role> profile;
    private AuthMiddleware auth;

    @BeforeEach
    public void setup() {
        auth = new AuthMiddleware(authProvider);
        when(routingContext.request()).thenReturn(request);
    }

    @Test
    public void authHandlerNoAuthorizationHeader() {
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
