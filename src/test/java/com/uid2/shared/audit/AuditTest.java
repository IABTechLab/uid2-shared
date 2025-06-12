package com.uid2.shared.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import static org.assertj.core.api.Assertions.assertThat;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AuditTest {

    private RoutingContext mockCtx;
    private HttpServerRequest mockRequest;
    private HttpServerResponse mockResponse;
    private SocketAddress mockAddress;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        mockCtx = Mockito.mock(RoutingContext.class);
        mockRequest = Mockito.mock(HttpServerRequest.class);
        mockResponse = Mockito.mock(HttpServerResponse.class);
        mockAddress = Mockito.mock(SocketAddress.class);

        Mockito.when(mockCtx.request()).thenReturn(mockRequest);
        Mockito.when(mockCtx.response()).thenReturn(mockResponse);
        Mockito.when(mockRequest.getHeader("User-Agent")).thenReturn("JUnit-Test-Agent");
        Mockito.when(mockRequest.remoteAddress()).thenReturn(mockAddress);
        Mockito.when(mockAddress.host()).thenReturn("127.0.0.1");
        Mockito.when(mockResponse.getStatusCode()).thenReturn(200);

        logger = (Logger) LoggerFactory.getLogger(Audit.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    public void testAudit() throws JsonProcessingException {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.GET);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("GET"))
                .findFirst();
        String auditLog = maybeEvent.get().getFormattedMessage();

        assertThat(auditLog.contains("method=GET")).isTrue();
        assertThat(auditLog.contains("status=200")).isTrue();
        assertThat(auditLog.contains("source=admin")).isTrue();
        assertThat(auditLog.contains("actor.user_agent=JUnit-Test-Agent")).isTrue();
        assertThat(auditLog.contains("actor.ip=127.0.0.1")).isTrue();

    }

    @Test
    public void testAuditFailSilently() {
        Mockito.when(mockCtx.request()).thenReturn(null);
        new Audit("admin").log(mockCtx, new AuditParams());

        boolean warnLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("Failed"));

        assertThat(warnLogged).isTrue();
    }

    @Test
    public void testAuditThrowsExceptionWhenSourceIsNotSpecified() {
        try {
            new Audit("admin").log(mockCtx, null);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testBodyParamsAsJsobObject() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("name.first", "location"));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonObject json = new JsonObject()
                .put("name", new JsonObject().put("first", "uid2_user"))
                .put("location", "seattle");

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("uid2_user") && msg.contains("seattle"));
    }

    @Test
    public void testBodyParamsAsJsobObjectWithSelectedBodyParams() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("name.first"));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonObject json = new JsonObject()
                .put("name", new JsonObject().put("first", "uid2_user"))
                .put("location", "seattle");

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("uid2_user"));
        assertThat(messages).noneMatch(msg -> msg.contains("seattle"));
    }

    @Test
    public void testBodyParamsAsJsonArray() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("partner_id", "config"));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonArray json = new JsonArray()
                .add(new JsonObject().put("partner_id", "1").put("config", "config1"))
                .add(new JsonObject().put("partner_id", "2").put("config", "config2"));

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("partner_id") && msg.contains("config"));
    }

    @Test
    public void testBodyParamsAsJsonArrayWithSelectedBodyParams() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("config"));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonArray json = new JsonArray()
                .add(new JsonObject().put("partner_id", "1").put("config", "config1"))
                .add(new JsonObject().put("partner_id", "2").put("config", "config2"));

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).noneMatch(msg -> msg.contains("partner_id"));
        assertThat(messages).allMatch(msg -> msg.contains("config"));
    }

    @Test
    public void testQueryParams() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(Arrays.asList("location"), null);

        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        queryParams.add("location", "seattle");

        Mockito.when(mockCtx.request().params()).thenReturn(queryParams);
        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains("seattle"));
    }
}
