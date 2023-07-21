package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
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
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.net.Proxy;
import java.security.SecureRandom;

import static com.uid2.shared.TestUtilites.makeInputStream;
import static org.mockito.Mockito.*;

public class AttestationTokenRetrieverTest {
    private String attestationEndpoint = "https://core-test.uidapi.com/attest";
    private String userToken;
    private ApplicationVersion appVersion = new ApplicationVersion("appName", "appVersion");
    private Proxy proxy;
    private IAttestationProvider attestationProvider = mock(IAttestationProvider.class);
    private boolean enforceHttps;
    private boolean allowContentFromLocalFileSystem;
    private AtomicReference<Handler<Integer>> responseWatcher = mock(AtomicReference.class);
    private IClock clock = mock(IClock.class);
    private static final long A_HUNDRED_DAYS_IN_MILLI = 86400000000L;

    private AttestationTokenRetriever attestationTokenRetriever =
            new AttestationTokenRetriever(attestationEndpoint, userToken, appVersion, proxy,
                    attestationProvider, enforceHttps, allowContentFromLocalFileSystem, responseWatcher, clock);

    public AttestationTokenRetrieverTest() throws IOException {
    }

    @Test
    public void testCurrentTimeBeforeTenMinsBeforeAttestationTokenExpiry() throws UidCoreClientException, IOException {
        AttestationTokenRetriever attestationTokenRetrieverSpy = spy(this.attestationTokenRetriever);

        Instant fakeExpiresAt = Instant.ofEpochMilli(A_HUNDRED_DAYS_IN_MILLI);
        when(clock.now()).thenReturn(fakeExpiresAt.minusSeconds(600).minusSeconds(100));
        attestationTokenRetrieverSpy.setAttestationTokenExpiresAt(fakeExpiresAt.toString());

        attestationTokenRetrieverSpy.attestationExpirationCheck();
        verify(attestationTokenRetrieverSpy, times(0)).attestInternal();
    }

    @Test
    public void testCurrentTimeAfterTenMinsBeforeAttestationTokenExpiry() throws UidCoreClientException, IOException {
        AttestationTokenRetriever attestationTokenRetrieverSpy = spy(attestationTokenRetriever);

        Instant fakeExpiresAt = Instant.ofEpochMilli(A_HUNDRED_DAYS_IN_MILLI);
        when(clock.now()).thenReturn(fakeExpiresAt.minusSeconds(600).plusSeconds(100));
        attestationTokenRetrieverSpy.setAttestationTokenExpiresAt(fakeExpiresAt.toString());

        attestationTokenRetrieverSpy.attestationExpirationCheck();
        verify(attestationTokenRetrieverSpy, times(1)).attestInternal();
    }

    @Test
    public void testAttestIfRequired() throws IOException, UidCoreClientException, AttestationException {
        HttpURLConnection mockConn1 = mock(HttpURLConnection.class);
        when(mockConn1.getResponseCode()).thenReturn(401);

        HttpURLConnection mockConn2 = mock(HttpURLConnection.class);
        OutputStream mockOutputStreamRequest = mock(OutputStream.class);
        JsonObject content = new JsonObject();
        JsonObject body = new JsonObject();
        body.put("expiresAt", "1970-01-01T00:00:00.111Z");
        body.put("attestation_token", "CPehNtWPehNtWABAMBFRACBoALAAAEJAAIYgAKwAQAKgArABAAqAAA");
        content.put("body", body);
        content.put("status", "success");
        when(mockConn2.getOutputStream()).thenReturn(mockOutputStreamRequest);
        when(mockConn2.getResponseCode()).thenReturn(200);
        when(mockConn2.getInputStream()).thenReturn(new ByteArrayInputStream(content.toString().getBytes()));

        URL mockURL = mock(URL.class);
        when(mockURL.openConnection()).thenReturn(mockConn2);
        when(mockURL.openConnection(any())).thenReturn(mockConn2);
//        attestationTokenRetriever.setUrl(mockURL);

        SecureRandom random = new SecureRandom(); // Default constructor uses a secure seed
        random.setSeed(123456L);

        byte[] fakeByteArray = {};
        when(attestationProvider.getAttestationRequest(any())).thenReturn(fakeByteArray);

        AttestationTokenRetriever attestationTokenRetrieverSpy = spy(attestationTokenRetriever);
//        boolean result = attestationTokenRetrieverSpy.attestIfRequired(mockConn1);
        verify(attestationTokenRetrieverSpy, times(1)).attestInternal();
//        Assert.assertEquals(true, result);
    }
}
