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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class AttestationTokenRetrieverTest {
    private String attestationEndpoint = "https://core-test.uidapi.com/attest";
    private ApplicationVersion appVersion = new ApplicationVersion("appName", "appVersion");
    private IAttestationProvider attestationProvider = mock(IAttestationProvider.class);
    private Handler<Integer> responseWatcher = mock(Handler.class);
    private IClock clock = mock(IClock.class);
    private HttpClient mockHttpClient = mock(HttpClient.class);
    private AttestationTokenDecryptor mockAttestationTokenDecryptor = mock(AttestationTokenDecryptor.class);
    private AttestationTokenRetriever attestationTokenRetriever =
            new AttestationTokenRetriever(attestationEndpoint, appVersion, attestationProvider, responseWatcher, clock, mockHttpClient, mockAttestationTokenDecryptor);

    public AttestationTokenRetrieverTest() throws IOException {
    }

    @Test
    public void Attest_Succeed_AttestationTokenSet(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever.setVertx(vertx);

        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"test\", \"expiresAt\": \"2023-08-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        testContext.completeNow();
        Assertions.assertEquals("test_attestation_token", attestationTokenRetriever.getAttestationToken());
    }

    @Test
    public void Attest_CurrentTimeAfterTenMinsBeforeAttestationTokenExpiry_ExpiryCheckCallsAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever.setVertx(vertx);

        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"test\", \"expiresAt\": \"2023-08-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        HttpResponse<String> mockHttpResponseSecondAttest = mock(HttpResponse.class);
        String expectedResponseBodySecondAttest = "{\"attestation_token\": \"test\", \"expiresAt\": \"2023-08-01T00:00:00.111Z\", \"status\": \"success\"}";
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
        testContext.completeNow();
    }

    @Test
    public void Attest_CurrentTimeAfterTenMinsBeforeAttestationTokenExpiry_ExpiryCheckDoesNotCallAttest(Vertx vertx, VertxTestContext testContext) throws Exception {
        attestationTokenRetriever.setVertx(vertx);

        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"test\", \"expiresAt\": \"2023-08-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        when(clock.now()).thenReturn(Instant.parse("2023-08-01T00:00:00.111Z").minusSeconds(600).minusSeconds(100));

        attestationTokenRetriever.attest();
        testContext.awaitCompletion(1, TimeUnit.SECONDS);
        // Verify on httpClient because we can't mock attestationTokenRetriever
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        testContext.completeNow();
    }

    @Test
    public void Attest_ResponseBodyHasNoAttestationToken_ExceptionThrown() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "2023-08-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"expiresAt\": \"2023-08-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assertions.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.attestation_token";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Attest_ResponseBodyHasNoExpiredAt_ExceptionThrown() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "2023-08-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assertions.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.expiresAt";
        Assertions.assertEquals(expectedExceptionMessage, result.getMessage());
    }
}
