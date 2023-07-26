package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
import com.uid2.shared.cloud.CloudStorageException;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.net.Proxy;
import java.security.SecureRandom;

import static com.uid2.shared.TestUtilites.makeInputStream;
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

    private AttestationTokenRetriever attestationTokenRetriever =
            new AttestationTokenRetriever(attestationEndpoint, appVersion, attestationProvider, responseWatcher, clock, mockHttpClient, mockAttestationTokenDecryptor);

    public AttestationTokenRetrieverTest() throws IOException {
    }

//    @Test
//    public void testCurrentTimeBeforeTenMinsBeforeAttestationTokenExpiry() throws UidCoreClientException, IOException {
//        AttestationTokenRetriever attestationTokenRetrieverSpy = spy(this.attestationTokenRetriever);
//
//        Instant fakeExpiresAt = Instant.ofEpochMilli(A_HUNDRED_DAYS_IN_MILLI);
//        when(clock.now()).thenReturn(fakeExpiresAt.minusSeconds(600).minusSeconds(100));
//        attestationTokenRetrieverSpy.setAttestationTokenExpiresAt(fakeExpiresAt.toString());
//
//        attestationTokenRetrieverSpy.attestationExpirationCheck();
//        verify(attestationTokenRetrieverSpy, times(0)).attestInternal();
//    }
//
//    @Test
//    public void testCurrentTimeAfterTenMinsBeforeAttestationTokenExpiry() throws UidCoreClientException, IOException {
//        AttestationTokenRetriever attestationTokenRetrieverSpy = spy(attestationTokenRetriever);
//
//        Instant fakeExpiresAt = Instant.ofEpochMilli(A_HUNDRED_DAYS_IN_MILLI);
//        when(clock.now()).thenReturn(fakeExpiresAt.minusSeconds(600).plusSeconds(100));
//        attestationTokenRetrieverSpy.setAttestationTokenExpiresAt(fakeExpiresAt.toString());
//
//        attestationTokenRetrieverSpy.attestationExpirationCheck();
//        verify(attestationTokenRetrieverSpy, times(1)).attestInternal();
//    }

    @Test
    public void testAttestInternalSuccess() throws Exception {
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

        attestationTokenRetriever.attestInternal();
        Assert.assertEquals("test_attestation_token", attestationTokenRetriever.getAttestationToken());
    }

    @Test
    public void testAttestInternalFailedWithoutAttestationToken() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
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
            attestationTokenRetriever.attestInternal();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.attestation_token";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }

    @Test
    public void testAttestInternalFailedWithoutExpiredAt() throws IOException, AttestationException, AttestationTokenRetrieverException, InterruptedException {
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
            attestationTokenRetriever.attestInternal();
        });
        String expectedExceptionMessage = "com.uid2.shared.attest.AttestationTokenRetrieverException: http status: 200, response json does not contain body.expiresAt";
        Assert.assertEquals(expectedExceptionMessage, result.getMessage());
    }
}
