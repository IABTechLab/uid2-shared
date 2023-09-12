package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.utils.Pair;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class AttestationTokenRetrieverTest {
    private static final String ATTESTATION_ENDPOINT = "https://core-test.uidapi.com/attest";
    private static final ApplicationVersion APP_VERSION = new ApplicationVersion("appName", "appVersion", new HashMap<String, String>()
    {{
        put("Component1", "Value1");
        put("Component2", "Value2");
    }});

    private final IAttestationProvider attestationProvider = mock(IAttestationProvider.class);
    private final Handler<Pair<Integer, String>> responseWatcher = mock(Handler.class);
    private final IClock clock = mock(IClock.class);
    private final HttpClient mockHttpClient = mock(HttpClient.class);
    private final AttestationTokenDecryptor mockAttestationTokenDecryptor = mock(AttestationTokenDecryptor.class);

    private AttestationTokenRetriever attestationTokenRetriever = null;

    @BeforeEach
    void setUp() {
        when(clock.now()).thenReturn(Clock.systemUTC().instant().plusSeconds(100));
    }

    @Test
    public void attest_succeed_attestationTokenSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z"));

        attestationTokenRetriever.attest();
        Assertions.assertEquals("test_attestation_token", attestationTokenRetriever.getAttestationToken());
        Assertions.assertEquals("appName=appVersion;Component1=Value1;Component2=Value2", attestationTokenRetriever.getAppVersionHeader());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_expiryCheckCallsAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\", \"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        HttpResponse<String> mockHttpResponseSecondAttest = mock(HttpResponse.class);
        String expectedResponseBodySecondAttest = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponseSecondAttest.body()).thenReturn(expectedResponseBodySecondAttest);
        when(mockHttpResponseSecondAttest.statusCode()).thenReturn(300);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse, mockHttpResponseSecondAttest);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        Instant tenSecondsAfterTenMinutesBeforeExpiry = Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).plusSeconds(10);
        Instant tenSecondsBeforeTenMinutesBeforeExpiry = Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).minusSeconds(10);
        when(clock.now()).thenReturn(tenSecondsAfterTenMinutesBeforeExpiry, tenSecondsBeforeTenMinutesBeforeExpiry);

        attestationTokenRetriever.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        verify(this.responseWatcher, times(1)).handle(Pair.of(300, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_expiryCheckDoesNotCallAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).minusSeconds(100));

        attestationTokenRetriever.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_providerNotReadyDoesNotCallAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        // isReady will be called twice:
        // The first is by attest() with true returned so that expiration check will be scheduled.
        // The second is by attestationExpirationCheck() with false, so that current check will be skipped.
        when(attestationProvider.isReady()).thenReturn(true, false);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).plusSeconds(100));

        attestationTokenRetriever.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.responseWatcher, only()).handle(Pair.of(200, ""));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_responseBodyHasNoAttestationToken_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "2023-08-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assertions.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.attestation_token";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_responseBodyHasNoExpiredAt_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "2023-08-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\", \"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assertions.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.expiresAt";

        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    @Test
    public void attest_providerNotReady_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(false);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        AttestationTokenRetrieverException result = Assertions.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "attestation provider is not ready";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());

        testContext.completeNow();
    }
    @Test
    public void attest_succeed_optOutJwtSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"test_jwt\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        Assertions.assertEquals("test_jwt", attestationTokenRetriever.getOptOutJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }
    @Test
    public void attest_succeed_coreJwtSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"test_jwt_core\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        Assertions.assertEquals("test_jwt_core", attestationTokenRetriever.getCoreJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }
    @Test
    public void attest_succeed_jwtsNull(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        Assertions.assertNull(attestationTokenRetriever.getOptOutJWT());
        Assertions.assertNull(attestationTokenRetriever.getCoreJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, ""));
        testContext.completeNow();
    }

    private AttestationTokenRetriever getAttestationTokenRetriever(Vertx vertx) {
        return new AttestationTokenRetriever(vertx, ATTESTATION_ENDPOINT, "testApiKey", APP_VERSION, attestationProvider, responseWatcher, clock, mockHttpClient, mockAttestationTokenDecryptor);
    }
}
