package com.uid2.shared.attest;

import com.amazonaws.util.Base64;
import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
import com.uid2.shared.cloud.CloudUtils;
import com.uid2.shared.util.URLConnectionHttpClient;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.utils.Pair;

import java.io.IOException;
import java.net.Proxy;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class AttestationResponseHandlerTest {
    private static final String ATTESTATION_ENDPOINT = "https://core-test.uidapi.com/attest";
    private byte[] ENCODED_ATTESTATION_ENDPOINT;
    private static final ApplicationVersion APP_VERSION = new ApplicationVersion("appName", "appVersion", new HashMap<String, String>()
    {{
        put("Component1", "Value1");
        put("Component2", "Value2");
    }});

    private final IAttestationProvider attestationProvider = mock(IAttestationProvider.class);
    private final Handler<Pair<Integer, String>> responseWatcher = mock(Handler.class);
    private final IClock clock = mock(IClock.class);
    private final URLConnectionHttpClient mockHttpClient = mock(URLConnectionHttpClient.class);
    private Proxy proxy = CloudUtils.defaultProxy;
    private final AttestationTokenDecryptor mockAttestationTokenDecryptor = mock(AttestationTokenDecryptor.class);

    private AttestationResponseHandler attestationResponseHandler = null;

    @BeforeEach
    void setUp() {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(ATTESTATION_ENDPOINT);
        ENCODED_ATTESTATION_ENDPOINT = Arrays.copyOf(buffer.array(), buffer.limit());
        when(clock.now()).thenReturn(Clock.systemUTC().instant().plusSeconds(100));
    }

    @Test
    public void attest_succeed_attestationTokenSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z"));

        attestationResponseHandler.attest();
        Assertions.assertEquals("test_attestation_token", attestationResponseHandler.getAttestationToken());
        Assertions.assertEquals("appName=appVersion;Component1=Value1;Component2=Value2", attestationResponseHandler.getAppVersionHeader());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_expiryCheckCallsAttestFixedIntervalUntilSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.statusCode()).thenReturn(200, 500, 500, 200);
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody, "bad", "bad", expectedResponseBody);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        Instant tenSecondsAfterTenMinutesBeforeExpiry = Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).plusSeconds(10);
        Instant tenSecondsBeforeTenMinutesBeforeExpiry = Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).minusSeconds(10);
        when(clock.now()).thenReturn(tenSecondsBeforeTenMinutesBeforeExpiry, tenSecondsAfterTenMinutesBeforeExpiry, tenSecondsAfterTenMinutesBeforeExpiry, tenSecondsAfterTenMinutesBeforeExpiry, tenSecondsBeforeTenMinutesBeforeExpiry);

        attestationResponseHandler.attest();
        testContext.awaitCompletion(1100, TimeUnit.MILLISECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(4)).post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class));
        verify(this.responseWatcher, times(2)).handle(Pair.of(200, expectedResponseBody));
        verify(this.responseWatcher, times(2)).handle(Pair.of(500, "bad"));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_expiryCheckDoesNotCallAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(Map.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).minusSeconds(100));

        attestationResponseHandler.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(1)).post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_currentTimeAfterTenMinsBeforeAttestationTokenExpiry_providerNotReadyDoesNotCallAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        // isReady will be called twice:
        // The first is by attest() with true returned so that expiration check will be scheduled.
        // The second is by attestationExpirationCheck() with false, so that current check will be skipped.
        when(attestationProvider.isReady()).thenReturn(true, false);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-01T00:00:00.111Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).plusSeconds(100));

        attestationResponseHandler.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(1)).post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class));
        verify(this.responseWatcher, only()).handle(Pair.of(200, expectedResponseBody));
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_responseBodyHasNoAttestationToken_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws IOException, AttestationException, AttestationResponseHandlerException, InterruptedException {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

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

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);

        AttestationResponseHandlerException result = Assertions.assertThrows(AttestationResponseHandlerException.class, () -> {
            attestationResponseHandler.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationResponseHandlerException: http status: 200, response json does not contain body.attestation_token";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_responseBodyHasNoExpiredAt_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws IOException, AttestationException, AttestationResponseHandlerException, InterruptedException {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

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

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);

        AttestationResponseHandlerException result = Assertions.assertThrows(AttestationResponseHandlerException.class, () -> {
            attestationResponseHandler.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationResponseHandlerException: http status: 200, response json does not contain body.expiresAt";

        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_providerNotReady_exceptionThrown(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(false);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        AttestationResponseHandlerException result = Assertions.assertThrows(AttestationResponseHandlerException.class, () -> {
            attestationResponseHandler.attest();
        });
        String expectedExceptionMessage = "attestation provider is not ready";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());

        testContext.completeNow();
    }
    @Test
    public void attest_succeed_optOutJwtSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"test_jwt\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationResponseHandler.attest();
        Assertions.assertEquals("test_jwt", attestationResponseHandler.getOptOutJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }
    @Test
    public void attest_succeed_coreJwtSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"test_jwt_core\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationResponseHandler.attest();
        Assertions.assertEquals("test_jwt_core", attestationResponseHandler.getCoreJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }
    @Test
    public void attest_succeed_jwtsNull(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), any(String.class), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationResponseHandler.attest();
        Assertions.assertNull(attestationResponseHandler.getOptOutJWT());
        Assertions.assertNull(attestationResponseHandler.getCoreJWT());
        verify(this.responseWatcher, times(1)).handle(Pair.of(200, expectedResponseBody));
        testContext.completeNow();
    }

    @Test
    public void attest_succeed_jsonRequest_includes_attestUrl_in_attestation_request(Vertx vertx, VertxTestContext testContext) throws  Exception{
        attestationResponseHandler = getAttestationTokenRetriever(vertx);

        when(attestationProvider.isReady()).thenReturn(true);
        when(attestationProvider.getAttestationRequest(any(), eq(ENCODED_ATTESTATION_ENDPOINT))).thenReturn(ENCODED_ATTESTATION_ENDPOINT);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"body\": {\"attestation_token\": \"test\",\"expiresAt\": \"2023-08-03T09:09:30.608597Z\",\"attestation_jwt_optout\": \"\",\"attestation_jwt_core\": \"\"},\"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        ArgumentCaptor<String> bodyCapture = ArgumentCaptor.forClass(String.class);

        when(mockHttpClient.post(eq(ATTESTATION_ENDPOINT), bodyCapture.capture(), any(HashMap.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z"));

        attestationResponseHandler.attest();

        String body = bodyCapture.getValue();
        JsonObject jsonBody = new JsonObject(body);
        Assertions.assertNotNull(jsonBody.getString("attestation_request"));
        String base64Content = jsonBody.getString("attestation_request");
        byte[] data = Base64.decode(base64Content);
        String decodedUrl = new String(data, StandardCharsets.UTF_8);
        Assertions.assertEquals(ATTESTATION_ENDPOINT, decodedUrl);

        verify(attestationProvider, times(1)).getAttestationRequest(any(), eq(ENCODED_ATTESTATION_ENDPOINT));

        testContext.completeNow();
    }

    private AttestationResponseHandler getAttestationTokenRetriever(Vertx vertx) {
        return new AttestationResponseHandler(vertx, ATTESTATION_ENDPOINT, "testApiKey", APP_VERSION, attestationProvider, responseWatcher, proxy, clock, mockHttpClient, mockAttestationTokenDecryptor, 250);
    }
}