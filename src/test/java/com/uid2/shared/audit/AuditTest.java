package com.uid2.shared.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import static org.assertj.core.api.Assertions.assertThat;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpMethod;

public class AuditTest {

    @Test
    public void test_audit() {
        // Arrange
        RoutingContext mockCtx = Mockito.mock(RoutingContext.class);
        HttpServerRequest mockRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(mockCtx.request()).thenReturn(mockRequest);
        Mockito.when(mockRequest.getHeader("User-Agent")).thenReturn("JUnit-Test-Agent");
        SocketAddress mockAddress = Mockito.mock(SocketAddress.class);
        Mockito.when(mockAddress.toString()).thenReturn("127.0.0.1");
        Mockito.when(mockRequest.remoteAddress()).thenReturn(mockAddress);
        HttpServerResponse mockResponse = Mockito.mock(HttpServerResponse.class);
        Mockito.when(mockCtx.response()).thenReturn(mockResponse);
        Mockito.when(mockResponse.getStatusCode()).thenReturn(200);
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.GET);
        AuditParams params = null;

        // Prepare logger capture
        Logger logger = (Logger) LoggerFactory.getLogger(Audit.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Audit audit = new Audit();
        audit.log(mockCtx, params);

        boolean found = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("GET"));

        assertThat(found).isTrue();
    }

    @Test
    public void test_audit_fail_silently() {
        RoutingContext mockCtx = Mockito.mock(RoutingContext.class);
        AuditParams params = null;

        Logger logger = (Logger) LoggerFactory.getLogger(Audit.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Audit audit = new Audit();
        audit.log(mockCtx, params);

        boolean warnLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("Failed"));

        assertThat(warnLogged).isTrue();
    }
}
