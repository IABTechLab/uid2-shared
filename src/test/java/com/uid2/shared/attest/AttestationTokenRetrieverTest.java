package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.IClock;
import io.vertx.core.Handler;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.net.Proxy;

import static org.mockito.Mockito.*;

public class AttestationTokenRetrieverTest {
    private String attestationEndpoint;
    private String userToken;
    private ApplicationVersion appVersion = new ApplicationVersion("appName", "appVersion");
    private Proxy proxy;
    private IAttestationProvider attestationProvider;
    private boolean enforceHttps;
    private boolean allowContentFromLocalFileSystem;
    private AtomicReference<Handler<Integer>> responseWatcher = mock(AtomicReference.class);
    private IClock clock = mock(IClock.class);
    private static final long A_HUNDRED_DAYS_IN_MILLI = 86400000000L;

    private AttestationTokenRetriever attestationTokenRetriever =
            new AttestationTokenRetriever(attestationEndpoint, userToken, appVersion, proxy,
                    attestationProvider, enforceHttps, allowContentFromLocalFileSystem, responseWatcher, clock);

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
}
