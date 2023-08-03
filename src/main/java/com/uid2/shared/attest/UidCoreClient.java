package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.InstantClock;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.*;
import com.uid2.shared.middleware.AttestationMiddleware;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class UidCoreClient implements IUidCoreClient, DownloadCloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClient.class);
    private final ICloudStorage contentStorage;
    private final Proxy proxy;
    private String userToken;
    private final String appVersionHeader;
    private boolean enforceHttps;
    private boolean allowContentFromLocalFileSystem = false;
    private HttpClient httpClient;
    private AttestationTokenRetriever attestationTokenRetriever;
    private Handler<Integer> responseWatcher;

    public static UidCoreClient createNoAttest(String attestationEndpoint, String userToken, ApplicationVersion appVersion, boolean enforceHttps) throws IOException {
        return new UidCoreClient(attestationEndpoint, userToken, appVersion, CloudUtils.defaultProxy, new NoAttestationProvider(), enforceHttps, null, null);
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps, HttpClient httpClient,
                         AttestationTokenRetriever attestationTokenRetriever) throws IOException {
        this.proxy = proxy;
        this.userToken = userToken;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.enforceHttps = enforceHttps;
        if (httpClient == null) {
            this.httpClient = HttpClient.newHttpClient();
        } else {
            this.httpClient = httpClient;
        }
        if (attestationTokenRetriever == null) {
            this.attestationTokenRetriever = new AttestationTokenRetriever(
                    attestationEndpoint, appVersion, attestationProvider, responseWatcher, new InstantClock(), null, null);
        } else {
            this.attestationTokenRetriever = attestationTokenRetriever;
        }

        String appVersionHeader = appVersion.getAppName() + "=" + appVersion.getAppVersion();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet())
            appVersionHeader += ";" + kv.getKey() + "=" + kv.getValue();
        this.appVersionHeader = appVersionHeader;
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        try {
            InputStream inputStream;
            if (allowContentFromLocalFileSystem && path.startsWith("file:/tmp/uid2")) {
                // returns `file:/tmp/uid2` urlConnection directly
                inputStream = readContentFromLocalFileSystem(path, this.proxy);
            } else {
                inputStream = getWithAttest(path);
            }
            return inputStream;
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }
    }

    private InputStream readContentFromLocalFileSystem(String path, Proxy proxy) throws IOException {
        return (proxy == null ? new URL(path).openConnection() : new URL(path).openConnection(proxy)).getInputStream();
    }

    private InputStream getWithAttest(String path) throws IOException, InterruptedException, AttestationTokenRetrieverException {
        if (!attestationTokenRetriever.attested()) {
            attestationTokenRetriever.attest();
        }

        String attestationToken = attestationTokenRetriever.getAttestationToken();
        String attestationJWT = attestationTokenRetriever.getAttestationJWT();

        HttpResponse<String> httpResponse;
        httpResponse = sendHttpRequest(path, attestationToken, attestationJWT);

        // This should never happen, but keeping this part of the code just to be extra safe.
        if (httpResponse.statusCode() == 401) {
            LOGGER.info("Initial response from UID2 Core returned 401, performing attestation");
            attestationTokenRetriever.attest();
            attestationToken = attestationTokenRetriever.getAttestationToken();
            httpResponse = sendHttpRequest(path, attestationToken, attestationJWT);
        }

        return Utils.convertHttpResponseToInputStream(httpResponse);
    }

    private HttpResponse sendHttpRequest(String path, String attestationToken, String attestationJWT) throws IOException, InterruptedException {
        URI uri = URI.create(path);
        if (this.enforceHttps && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("UidCoreClient requires HTTPS connection");
        }

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .setHeader(Const.Http.AppVersionHeader, appVersionHeader);

        if (this.userToken != null && !this.userToken.isBlank()) {
            httpRequestBuilder.setHeader("Authorization", "Bearer " + this.userToken);
        }
        if (attestationToken != null && !attestationToken.isBlank()) {
            httpRequestBuilder.setHeader(Const.Attestation.AttestationTokenHeader, attestationToken);
        }
        if (attestationJWT != null && !attestationJWT.isBlank()) {
            httpRequestBuilder.setHeader(Const.Attestation.AttestationJWTHeader, attestationJWT);
        }
        HttpRequest httpRequest = httpRequestBuilder.build();
        HttpResponse<String> httpResponse = null;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to send request with error: ", e);
            throw e;
        }
        return httpResponse;
    }

    public void setResponseStatusWatcher(Handler<Integer> watcher) {
        this.responseWatcher = watcher;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public void setAllowContentFromLocalFileSystem(boolean allowContentFromLocalFileSystem) {
        this.allowContentFromLocalFileSystem = allowContentFromLocalFileSystem;
    }
}
