package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.*;
import com.uid2.shared.util.URLConnectionHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class UidCoreClient implements IUidCoreClient, DownloadCloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClient.class);
    private final ICloudStorage contentStorage;
    private final Proxy proxy;
    private final URLConnectionHttpClient httpClient;
    private String userToken;
    private final String appVersionHeader;
    private final boolean enforceHttps;
    private boolean allowContentFromLocalFileSystem = false;
    private final AttestationTokenRetriever attestationTokenRetriever;


    public static UidCoreClient createNoAttest(String userToken, boolean enforceHttps, AttestationTokenRetriever attestationTokenRetriever) {
        return new UidCoreClient(userToken, CloudUtils.defaultProxy, enforceHttps, attestationTokenRetriever, null);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         boolean enforceHttps,
                         AttestationTokenRetriever attestationTokenRetriever) {
        this(userToken, proxy, enforceHttps, attestationTokenRetriever, null);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         boolean enforceHttps,
                         AttestationTokenRetriever attestationTokenRetriever,
                         URLConnectionHttpClient httpClient) {
        this.proxy = proxy;
        this.userToken = userToken;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.enforceHttps = enforceHttps;
        if (httpClient == null) {
            this.httpClient = new URLConnectionHttpClient(proxy);
        } else {
            this.httpClient = httpClient;
        }
        if (attestationTokenRetriever == null) {
            throw new IllegalArgumentException("attestationTokenRetriever can not be null");
        } else {
            this.attestationTokenRetriever = attestationTokenRetriever;
        }

        this.appVersionHeader = attestationTokenRetriever.getAppVersionHeader();
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        String coreJWT = this.getJWT();
        return this.internalDownload(path, coreJWT);
    }

    @Deprecated
    public InputStream downloadFromOptOut(String path) throws CloudStorageException {
        String optOutJWT = attestationTokenRetriever.getOptOutJWT();
        return this.internalDownload(path, optOutJWT);
    }

    protected String getJWT() {
        return this.getAttestationTokenRetriever().getCoreJWT();
    }

    private InputStream internalDownload(String path, String jwtToken) throws CloudStorageException {
        try {
            InputStream inputStream;
            if (allowContentFromLocalFileSystem && path.startsWith("file:/tmp/uid2")) {
                // returns `file:/tmp/uid2` urlConnection directly
                inputStream = readContentFromLocalFileSystem(path, this.proxy);
            } else {
                inputStream = getWithAttest(path, jwtToken);
            }
            return inputStream;
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }

    }

    private InputStream readContentFromLocalFileSystem(String path, Proxy proxy) throws IOException {
        return (proxy == null ? new URL(path).openConnection() : new URL(path).openConnection(proxy)).getInputStream();
    }

    private InputStream getWithAttest(String path, String jwtToken) throws IOException, InterruptedException, AttestationTokenRetrieverException {
        if (!attestationTokenRetriever.attested()) {
            attestationTokenRetriever.attest();
        }

        String attestationToken = attestationTokenRetriever.getAttestationToken();
        if (jwtToken == null || jwtToken.isEmpty()) {
            jwtToken = this.getJWT();
        }

        HttpResponse<String> httpResponse;
        httpResponse = sendHttpRequest(path, attestationToken, jwtToken);

        // This should never happen, but keeping this part of the code just to be extra safe.
        if (httpResponse.statusCode() == 401) {
            LOGGER.info("Initial response from UID2 Core returned 401, performing attestation");
            attestationTokenRetriever.attest();
            attestationToken = attestationTokenRetriever.getAttestationToken();
            httpResponse = sendHttpRequest(path, attestationToken, jwtToken);
        }

        return Utils.convertHttpResponseToInputStream(httpResponse);
    }

    private HttpResponse<String> sendHttpRequest(String path, String attestationToken, String attestationJWT) throws IOException {
        URI uri = URI.create(path);
        if (this.enforceHttps && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("UidCoreClient requires HTTPS connection");
        }

        HashMap<String, String> headers = new HashMap<>();
        headers.put(Const.Http.AppVersionHeader, this.appVersionHeader);
        if (this.userToken != null && !this.userToken.isBlank()) {
            headers.put("Authorization", "Bearer " + this.userToken);
        }
        if (attestationToken != null && !attestationToken.isBlank()) {
            headers.put(Const.Attestation.AttestationTokenHeader, attestationToken);
        }
        if (attestationJWT != null && !attestationJWT.isBlank()) {
            headers.put(Const.Attestation.AttestationJWTHeader, attestationJWT);
        }

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.get(path, headers);
        } catch (IOException e) {
            LOGGER.error("Failed to send request with error: ", e);
            throw e;
        }
        return httpResponse;
    }

    protected AttestationTokenRetriever getAttestationTokenRetriever() {
        return attestationTokenRetriever;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public void setAllowContentFromLocalFileSystem(boolean allowContentFromLocalFileSystem) {
        this.allowContentFromLocalFileSystem = allowContentFromLocalFileSystem;
    }
}
