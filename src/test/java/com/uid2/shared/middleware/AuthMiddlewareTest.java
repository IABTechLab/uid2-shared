package com.uid2.shared.middleware;

import com.uid2.shared.audit.Audit;
import com.uid2.shared.auth.IAuthorizableProvider;
import com.uid2.shared.auth.IRoleAuthorizable;
import com.uid2.shared.auth.OperatorKey;
import com.uid2.shared.auth.OperatorType;
import com.uid2.shared.auth.Role;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

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
    private OperatorKey operatorKey = new OperatorKey(null, null, "operator-key", "contact", "trusted", 1000, false, 999, Set.of(Role.OPERATOR), OperatorType.PUBLIC, "test-key-id");
    private AuthMiddleware auth;


    @BeforeEach
    public void setup() {
        auth = new AuthMiddleware(authProvider, "app");
        when(routingContext.request()).thenReturn(request);
    }

    @Test
    public void authHandlerNoAuthorizationHeader() {
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void authHandlerInvalidAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bogus Header Value");
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void authHandlerUnknownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer unknown-key");
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void authHandlerKeyWithoutRoles() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void authHandlerKeyWithFirstRole() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.MAPPER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test
    public void authHandlerKeyWithFirstRoleAudit() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.MAPPER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handleWithAudit(nextHandler, List.of(Role.MAPPER, Role.ID_READER));
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(1)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    private void verifyAuditLogFilled() {
        ArgumentCaptor<String> keyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(routingContext).put(keyArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture());
        JsonObject auditLogUserDetailsActual = jsonObjectArgumentCaptor.getValue();
        Assertions.assertEquals(Audit.USER_DETAILS, keyArgumentCaptor.getValue());
        Assertions.assertEquals(operatorKey.getName(), auditLogUserDetailsActual.getString("key_name"));
        Assertions.assertEquals(operatorKey.getSiteId().toString(), auditLogUserDetailsActual.getString("key_site_id"));
    }

    @Test
    public void authHandlerOperatorKeyWithFirstRoleAudit() {
        when(request.getHeader("Authorization")).thenReturn("Bearer operator-key");
        when(authProvider.get(operatorKey.getName())).thenReturn(operatorKey);
        Handler<RoutingContext> handler = auth.handleWithAudit(nextHandler, List.of(Role.OPERATOR));
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(1)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
        verifyAuditLogFilled();
    }

    @Test
    public void authHandlerOperatorKeyWithUnauthorizedRoleAudit() {
        when(request.getHeader("Authorization")).thenReturn("Bearer operator-key");
        when(authProvider.get(operatorKey.getName())).thenReturn(operatorKey);
        Handler<RoutingContext> handler = auth.handleWithAudit(nextHandler, List.of(Role.OPTOUT));
        handler.handle(routingContext);
        verifyNoInteractions(nextHandler);
        verify(routingContext).fail(401);
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
        verifyAuditLogFilled();
    }

    @Test public void authHandlerKeyWithSecondRole() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.ID_READER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handle(nextHandler, Role.MAPPER, Role.ID_READER);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test
    public void authHandlerKeyWithSecondRoleAudit() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        when(profile.hasRole(Role.ID_READER)).thenReturn(true);
        Handler<RoutingContext> handler = auth.handleWithAudit(nextHandler,  List.of(Role.MAPPER, Role.ID_READER));
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(1)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
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
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void noAuthHandlerNoAuthorizationHeader() {
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void noAuthHandlerInvalidAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bogus Header Value");
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void noAuthHandlerUnknownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }

    @Test public void noAuthHandlerKnownKey() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
        when(authProvider.get("some-key")).thenReturn(profile);
        Handler<RoutingContext> handler = auth.handleWithOptionalAuth(nextHandler);
        handler.handle(routingContext);
        verify(profile, times(0)).isDisabled();
        verify(nextHandler).handle(routingContext);
        verify(routingContext, times(0)).fail(any());
        verify(routingContext, times(0)).addBodyEndHandler(ArgumentMatchers.<Handler<Void>>any());
    }
}
