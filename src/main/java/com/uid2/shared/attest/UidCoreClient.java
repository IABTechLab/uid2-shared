package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.InstantClock;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.*;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class UidCoreClient implements IUidCoreClient, DownloadCloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClient.class);
    private final ICloudStorage contentStorage;
    private final String userToken;
    private final String appVersionHeader;
    private AtomicReference<String> attestationToken;
    private boolean allowContentFromLocalFileSystem = false;
    private HttpClient httpClient;
    private AttestationTokenRetriever attestationTokenRetriever;
    private AtomicReference<Handler<Integer>> responseWatcher;

    public static UidCoreClient createNoAttest(String attestationEndpoint, String userToken, ApplicationVersion appVersion, boolean enforceHttps) throws IOException {
        return new UidCoreClient(attestationEndpoint, userToken, appVersion, CloudUtils.defaultProxy, new NoAttestationProvider(), enforceHttps);
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps) throws IOException {
        this.userToken = userToken;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.attestationToken = new AtomicReference<>(null);
        this.responseWatcher = new AtomicReference<Handler<Integer>>(null);
        this.attestationTokenRetriever = new AttestationTokenRetriever(
                attestationEndpoint, userToken, appVersion, proxy, attestationProvider, enforceHttps,
                allowContentFromLocalFileSystem, responseWatcher, new InstantClock());
        this.httpClient = HttpClient.newHttpClient();

        String appVersionHeader = appVersion.getAppName() + "=" + appVersion.getAppVersion();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet())
            appVersionHeader += ";" + kv.getKey() + "=" + kv.getValue();
        this.appVersionHeader = appVersionHeader;
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps, HttpClient httpClient) throws IOException {
        this.userToken = userToken;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.attestationToken = new AtomicReference<>(null);
        this.responseWatcher = new AtomicReference<Handler<Integer>>(null);
        this.attestationTokenRetriever = new AttestationTokenRetriever(
                attestationEndpoint, userToken, appVersion, proxy, attestationProvider, enforceHttps,
                allowContentFromLocalFileSystem, responseWatcher, new InstantClock());
        this.httpClient = httpClient;

        String appVersionHeader = appVersion.getAppName() + "=" + appVersion.getAppVersion();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet())
            appVersionHeader += ";" + kv.getKey() + "=" + kv.getValue();
        this.appVersionHeader = appVersionHeader;
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    public void setAllowContentFromLocalFileSystem(boolean allow) {
        allowContentFromLocalFileSystem = allow;
    }

    public boolean attested() {
        return this.attestationToken.get() != null;
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        try {
            return getWithAttest(path);
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }
    }

    private InputStream getWithAttest(String path) throws IOException, UidCoreClientException, InterruptedException {
        if (!attested()) {
            attestationTokenRetriever.attestInternal();
            setAttestationToken(attestationTokenRetriever.getAttestationToken());
        }

        HttpResponse<String> httpResponse;
        httpResponse = sendHttpRequest(path, attestationToken.get());

        if (httpResponse.statusCode() == 401) {
            LOGGER.info("Initial response from UID2 Core returned 401, performing attestation");
            attestationTokenRetriever.notifyResponseStatusWatcher(401);
            attestationTokenRetriever.attestInternal();
            setAttestationToken(attestationTokenRetriever.getAttestationToken());
            httpResponse = sendHttpRequest(path, attestationToken.get());
        }

        attestationTokenRetriever.notifyResponseStatusWatcher(httpResponse.statusCode());

        return Utils.convertHttpResponseToInputStream(httpResponse);
    }

    private HttpResponse sendHttpRequest(String path, String attestationToken) throws IOException, InterruptedException {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .GET()
                .setHeader(Const.Http.AppVersionHeader, appVersionHeader);
//        if(enforceHttps && !(urlConnection instanceof HttpsURLConnection)) {
//            throw new IOException("UidCoreClient requires HTTPS connection");
//        }
//
//        if (allowContentFromLocalFileSystem && serviceEndpoint.startsWith("file:/tmp/uid2")) {
//            // returns `file:/tmp/uid2` urlConnection directly
//            return urlConnection;
//        }
        if(this.userToken != null && this.userToken.length() > 0) {
            httpRequestBuilder.setHeader("Authorization", "Bearer " + this.userToken);
        }
        if(attestationToken != null && attestationToken.length() > 0) {
            httpRequestBuilder.setHeader("Attestation-Token", attestationToken);
        }
        HttpRequest httpRequest = httpRequestBuilder.build();
        HttpResponse<String> httpResponse = null;
        try {
            System.out.println(HttpResponse.BodyHandlers.ofString());
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw e;
        }
        return httpResponse;
    }

    public void setResponseStatusWatcher(Handler<Integer> watcher) {
        this.responseWatcher.set(watcher);
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setAttestationTokenRetriever(AttestationTokenRetriever attestationTokenRetriever) {
        this.attestationTokenRetriever = attestationTokenRetriever;
    }

    public void setAttestationToken(String attestationToken) {
        this.attestationToken.set(attestationToken);
    }
}
