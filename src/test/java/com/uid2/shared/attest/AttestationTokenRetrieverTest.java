package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class AttestationTokenRetrieverTest {
    private String attestationEndpoint = "https://core-test.uidapi.com/attest";
    private ApplicationVersion appVersion = new ApplicationVersion("appName", "appVersion");
    private IAttestationProvider attestationProvider = mock(IAttestationProvider.class);
    private Handler<Integer> responseWatcher = mock(Handler.class);
    private IClock clock = mock(IClock.class);
    private static final long A_HUNDRED_DAYS_IN_MILLI = 86400000000L;
    private HttpClient mockHttpClient = mock(HttpClient.class);
    private AttestationTokenDecryptor mockAttestationTokenDecryptor = mock(AttestationTokenDecryptor.class);
    private ScheduledThreadPoolExecutor mockExecutor = mock(ScheduledThreadPoolExecutor.class);

    private AttestationTokenRetriever attestationTokenRetriever =
            new AttestationTokenRetriever(attestationEndpoint, appVersion, attestationProvider, responseWatcher, clock, mockHttpClient, mockAttestationTokenDecryptor, mockExecutor);

    public AttestationTokenRetrieverTest() throws IOException {
    }

    @Test
    public void Attest_Succeed_AttestationTokenSet() throws Exception {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "1970-01-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"test\", \"expiresAt\": \"1970-01-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        Assert.assertEquals("test_attestation_token", attestationTokenRetriever.getAttestationToken());
    }

    @Test
    public void Attest_Succeed_ExpiryCheckScheduled() throws Exception {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "1970-01-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"test\", \"expiresAt\": \"1970-01-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
        when(mockAttestationTokenDecryptor.decrypt(any(), any())).thenReturn("test_attestation_token".getBytes(StandardCharsets.UTF_8));

        attestationTokenRetriever.attest();
        verify(mockExecutor, times(1)).scheduleAtFixedRate(any(), eq((long) 0), eq(TimeUnit.MINUTES.toMillis(1)), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void Attest_ResponseBodyHasNoAttestationToken_ExceptionThrown() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "1970-01-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"expiresAt\": \"1970-01-01T00:00:00.111Z\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assert.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.attestation_token";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void Attest_ResponseBodyHasNoExpiredAt_ExceptionThrown() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
        when(attestationProvider.getAttestationRequest(any())).thenReturn(new byte[1]);

        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "1970-01-01T00:00:00.111Z");
        body.put("attestation_token", "pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g");
        content.put("body", body);
        content.put("status", "success");

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        String expectedResponseBody = "{\"attestation_token\": \"pdA9stfFBTWsJGwOPjOsaMR7G5+mkxhOcc9xFnAM3RfSOpnmclaQCMmdhgNDY1Egtl9ejZQrCEs=-8RiWE9OEheFDnFkZ-g\", \"status\": \"success\"}";
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AttestationTokenRetrieverException result = Assert.assertThrows(AttestationTokenRetrieverException.class, () -> {
            attestationTokenRetriever.attest();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.expiresAt";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }
}
