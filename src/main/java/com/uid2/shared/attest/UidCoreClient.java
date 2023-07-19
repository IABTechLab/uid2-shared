package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.cloud.*;
import io.vertx.core.Handler;

import java.io.InputStream;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicReference;

public class UidCoreClient implements IUidCoreClient, DownloadCloudStorage {
    private final ICloudStorage contentStorage;
    private final Proxy proxy;
    private boolean allowContentFromLocalFileSystem = false;
    private AttestationTokenRetriever attestationTokenRetriever;
    private AtomicReference<Handler<Integer>> responseWatcher;

    public static UidCoreClient createNoAttest(String attestationEndpoint, String userToken, ApplicationVersion appVersion, boolean enforceHttps) {
        return new UidCoreClient(attestationEndpoint, userToken, appVersion, CloudUtils.defaultProxy, new NoAttestationProvider(), enforceHttps);
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps) {
        this.proxy = proxy;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.attestationTokenRetriever = new AttestationTokenRetriever(attestationEndpoint, userToken, appVersion, proxy, attestationProvider, enforceHttps, allowContentFromLocalFileSystem, responseWatcher);
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
            return attestationTokenRetriever.getWithAttest(path);
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }
    }

    public void setResponseStatusWatcher(Handler<Integer> watcher) {
        this.responseWatcher.set(watcher);
    }
}
