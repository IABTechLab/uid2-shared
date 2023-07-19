package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.InstantClock;
import com.uid2.shared.cloud.*;
import io.vertx.core.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

public class UidCoreClient implements IUidCoreClient, DownloadCloudStorage {
    private final ICloudStorage contentStorage;
    private final Proxy proxy;
    private boolean allowContentFromLocalFileSystem = false;
    private AttestationTokenRetriever attestationTokenRetriever;
    private AtomicReference<Handler<Integer>> responseWatcher;

    public static UidCoreClient createNoAttest(String attestationEndpoint, String userToken, ApplicationVersion appVersion, boolean enforceHttps) throws IOException {
        return new UidCoreClient(attestationEndpoint, userToken, appVersion, CloudUtils.defaultProxy, new NoAttestationProvider(), enforceHttps);
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps) throws IOException {
        this.proxy = proxy;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.attestationTokenRetriever = new AttestationTokenRetriever(
                attestationEndpoint, userToken, appVersion, proxy, attestationProvider, enforceHttps,
                allowContentFromLocalFileSystem, responseWatcher, new InstantClock());
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    public void setAllowContentFromLocalFileSystem(boolean allow) {
        allowContentFromLocalFileSystem = allow;
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        try {
            return getWithAttest(path);
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }
    }

    private InputStream getWithAttest(String path) throws IOException, UidCoreClientException{
        if (!attestationTokenRetriever.attested())
            attestationTokenRetriever.attestInternal();

        URLConnection conn = attestationTokenRetriever.sendGet(path);

        if (conn instanceof HttpURLConnection && attestationTokenRetriever.attestIfRequired((HttpURLConnection) conn))
            conn = attestationTokenRetriever.sendGet(path);

        return conn.getInputStream();
    }

    public void setResponseStatusWatcher(Handler<Integer> watcher) {
        this.responseWatcher.set(watcher);
    }

    public void setAttestationTokenRetriever(AttestationTokenRetriever attestationTokenRetriever) {
        this.attestationTokenRetriever = attestationTokenRetriever;
    }
}
