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

import static com.uid2.shared.audit.Audit.UID_INSTANCE_ID_HEADER;
import static com.uid2.shared.audit.Audit.UID_TRACE_ID_HEADER;
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
    private String UID2_SECRET_KEY = "UID2-O-P-AB12cd34EF-zyX9_abCDEFghijklMNOPQRSTuvwxYZ0123";
    private String SECRET = "jhgjy6tuygiuhi";
    private String SQL_STATEMENT = "SELECT * FROM users WHERE username = '' OR '1'='1'";
    private String TRACE_ID = "Root=1-6825017b-2321f2302b5ea904340c1cff";
    private String UID_TRACE_ID = "Root=1-6825017b-2321f2302b5ea904340c1cfa";
    private String MALFORMED_TRACE_ID = "Root=1-6825017b-2321f2302b5ea904df340c1ckgg";
    private String AMZN_TRACE_ID_HEADER = "X-Amzn-Trace-Id";
    private String UID_INSTANCE_ID_FROM_INTEG = "uid2-integ-use2-operator-dfb4bd68d-v9p6t-a2cf5882f000d7b2";
    private String UID_INSTANCE_ID_FROM_PROD = "uid2-prod-use2-operator-6bb87b7fd-n4smk-90527e73fbffa91c";
    private String UID_INSTANCE_ID_FROM_AWS = "aws-aasdadada-ami-12312311321-v9p6t-a2cf5882f000d7b2";
    private String MALFORMED_UID_INSTANCE_ID = "uid2-prod-SELECT * FROM usersUID2-O-P-AB12cd34EF-zyX9_abCDEFghijklMNOPQRSTuvwxYZ0123";


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
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("method").asText()).isEqualTo("GET");
        assertThat(jsonNode.get("status").asInt()).isEqualTo(200);
        assertThat(jsonNode.get("source").asText()).isEqualTo("admin");

        JsonNode actor = jsonNode.get("actor");
        assertThat(actor).isNotNull();
        assertThat(actor.get("user_agent").asText()).isEqualTo("JUnit-Test-Agent");
        assertThat(actor.get("ip").asText()).isEqualTo("127.0.0.1");

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
    public void testBodyParamsAsJsobObjectWithSecret() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("name.first", "location", UID2_SECRET_KEY));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonObject json = new JsonObject()
                .put("name", new JsonObject().put("first", "uid2_user"))
                .put("location", "seattle")
                .put(UID2_SECRET_KEY, SECRET);

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("uid2_user") && msg.contains("seattle"));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: request_body."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testBodyParamsAsJsobObjectWithSecretNSQL() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("name.first", "location", UID2_SECRET_KEY, SQL_STATEMENT));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonObject json = new JsonObject()
                .put("name", new JsonObject().put("first", "uid2_user"))
                .put("location", "seattle")
                .put(UID2_SECRET_KEY, SECRET)
                .put(SQL_STATEMENT, "weather");

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("uid2_user") && msg.contains("seattle"));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));
        assertThat(messages).noneMatch(msg -> msg.contains(SQL_STATEMENT));
        assertThat(messages).noneMatch(msg -> msg.contains("weather"));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: request_body.") && event.getFormattedMessage().contains("SQL injection found in the audit log: request_body."));

        assertThat(errorLogged).isTrue();
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
    public void testBodyParamsAsJsonArrayWithSecret() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("partner_id", "config", UID2_SECRET_KEY));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonArray json = new JsonArray()
                .add(new JsonObject().put("partner_id", "1").put("config", "config1"))
                .add(new JsonObject().put("partner_id", "2").put("config", "config2"))
                .add(new JsonObject().put(UID2_SECRET_KEY, SECRET).put("config", "config3"));

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("partner_id") && msg.contains("config"));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: request_body."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testBodyParamsAsJsonArrayWithSecretNSQL() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(null, Arrays.asList("partner_id", "config", UID2_SECRET_KEY, "weather"));

        RequestBody mockBody = Mockito.mock(RequestBody.class);
        JsonArray json = new JsonArray()
                .add(new JsonObject().put("partner_id", "1").put("config", "config1"))
                .add(new JsonObject().put("partner_id", "2").put("config", "config2"))
                .add(new JsonObject().put(UID2_SECRET_KEY, SECRET).put("config", "config3"))
                .add(new JsonObject().put("weather", SQL_STATEMENT).put("config", "config3"));

        Mockito.when(mockCtx.body()).thenReturn(mockBody);
        Mockito.when(mockBody.buffer()).thenReturn(Buffer.buffer(json.toString()));

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).allMatch(msg -> msg.contains("partner_id") && msg.contains("config"));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));
        assertThat(messages).noneMatch(msg -> msg.contains(SQL_STATEMENT));
        assertThat(messages).noneMatch(msg -> msg.contains("weather"));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: request_body.") && event.getFormattedMessage().contains("SQL injection found in the audit log: request_body."));

        assertThat(errorLogged).isTrue();
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

    @Test
    public void testQueryParamsContainsSecrets() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(Arrays.asList(UID2_SECRET_KEY, "location"), null);

        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        queryParams.add(UID2_SECRET_KEY, SECRET);
        queryParams.add("location", "seattle");

        Mockito.when(mockCtx.request().params()).thenReturn(queryParams);
        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains("seattle"));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: query_params."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testQueryParamsContainsSQL() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(Arrays.asList("weather", "location"), null);

        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        queryParams.add("weather", SQL_STATEMENT);
        queryParams.add("location", "seattle");

        Mockito.when(mockCtx.request().params()).thenReturn(queryParams);
        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains("seattle"));
        assertThat(messages).noneMatch(msg -> msg.contains("weather"));
        assertThat(messages).noneMatch(msg -> msg.contains(SQL_STATEMENT));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("SQL injection found in the audit log: query_params."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testQueryParamsContainsSecretNSQL() {
        Mockito.when(mockRequest.method()).thenReturn(HttpMethod.POST);
        AuditParams params = new AuditParams(Arrays.asList(UID2_SECRET_KEY, "weather", "location"), null);

        MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
        queryParams.add("weather", SQL_STATEMENT);
        queryParams.add("location", "seattle");
        queryParams.add(UID2_SECRET_KEY, SECRET);

        Mockito.when(mockCtx.request().params()).thenReturn(queryParams);
        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains("seattle"));
        assertThat(messages).noneMatch(msg -> msg.contains("weather"));
        assertThat(messages).noneMatch(msg -> msg.contains(SQL_STATEMENT));
        assertThat(messages).noneMatch(msg -> msg.contains(UID2_SECRET_KEY));
        assertThat(messages).noneMatch(msg -> msg.contains(SECRET));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Secret found in the audit log: query_params.") && event.getFormattedMessage().contains("SQL injection found in the audit log: query_params."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testTraceId() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(AMZN_TRACE_ID_HEADER)).thenReturn(TRACE_ID);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("trace_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("trace_id").asText()).isEqualTo(TRACE_ID);
        assertThat(jsonNode.get("uid_trace_id").asText()).isEqualTo(TRACE_ID);
    }

    @Test
    public void testMalformedTraceId() {
        Mockito.when(mockRequest.getHeader(AMZN_TRACE_ID_HEADER)).thenReturn(MALFORMED_TRACE_ID);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains(MALFORMED_TRACE_ID));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Malformed trace_id found in the audit log. Malformed uid_trace_id found in the audit log."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testUIDTraceId() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(AMZN_TRACE_ID_HEADER)).thenReturn(TRACE_ID);
        Mockito.when(mockRequest.getHeader(UID_TRACE_ID_HEADER)).thenReturn(UID_TRACE_ID);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("trace_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("trace_id").asText()).isEqualTo(TRACE_ID);
        assertThat(jsonNode.get("uid_trace_id").asText()).isEqualTo(UID_TRACE_ID);
    }

    @Test
    public void testMalformedUIDTraceId() {
        Mockito.when(mockRequest.getHeader(AMZN_TRACE_ID_HEADER)).thenReturn(TRACE_ID);
        Mockito.when(mockRequest.getHeader(UID_TRACE_ID_HEADER)).thenReturn(MALFORMED_TRACE_ID);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).anyMatch(msg -> msg.contains(TRACE_ID));
        assertThat(messages).noneMatch(msg -> msg.contains(UID_TRACE_ID));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Malformed uid_trace_id found in the audit log."));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testMalformedUIDInstanceId() {
        Mockito.when(mockRequest.getHeader(UID_INSTANCE_ID_HEADER)).thenReturn(MALFORMED_UID_INSTANCE_ID);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(messages).noneMatch(msg -> msg.contains(TRACE_ID));

        boolean errorLogged = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Malformed uid_instance_id found in the audit log. "));

        assertThat(errorLogged).isTrue();
    }

    @Test
    public void testUIDInstanceIdNull() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(UID_INSTANCE_ID_HEADER)).thenReturn(null);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);


        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("uid_instance_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("uid_instance_id").asText()).isEqualTo("unknown");
    }

    @Test
    public void testUIDInstanceIdFromInteg() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(UID_INSTANCE_ID_HEADER)).thenReturn(UID_INSTANCE_ID_FROM_INTEG);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);


        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("uid_instance_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("uid_instance_id").asText()).isEqualTo(UID_INSTANCE_ID_FROM_INTEG);
    }

    @Test
    public void testUIDInstanceIdFromProd() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(UID_INSTANCE_ID_HEADER)).thenReturn(UID_INSTANCE_ID_FROM_PROD);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);


        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("uid_instance_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("uid_instance_id").asText()).isEqualTo(UID_INSTANCE_ID_FROM_PROD);
    }

    @Test
    public void testUIDInstanceIdFromAWS() throws JsonProcessingException {
        Mockito.when(mockRequest.getHeader(UID_INSTANCE_ID_HEADER)).thenReturn(UID_INSTANCE_ID_FROM_AWS);
        AuditParams params = new AuditParams();

        new Audit("admin").log(mockCtx, params);


        Optional<ILoggingEvent> maybeEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("uid_instance_id"))
                .findFirst();
        String jsonLog = maybeEvent.get().getFormattedMessage();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonLog);

        assertThat(jsonNode.get("uid_instance_id").asText()).isEqualTo(UID_INSTANCE_ID_FROM_AWS);
    }
}
