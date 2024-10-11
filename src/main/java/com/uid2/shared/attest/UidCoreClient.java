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
    private boolean allowContentFromLocalFileSystem = false;
    private final AttestationResponseHandler attestationResponseHandler;


    public static UidCoreClient createNoAttest(String userToken, AttestationResponseHandler attestationResponseHandler) {
        return new UidCoreClient(userToken, CloudUtils.defaultProxy, attestationResponseHandler, null);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         AttestationResponseHandler attestationResponseHandler) {
        this(userToken, proxy, attestationResponseHandler, null);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         AttestationResponseHandler attestationResponseHandler,
                         URLConnectionHttpClient httpClient) {
        this.proxy = proxy;
        this.userToken = userToken;
        this.contentStorage = new PreSignedURLStorage(proxy);
        if (httpClient == null) {
            this.httpClient = new URLConnectionHttpClient(proxy);
        } else {
            this.httpClient = httpClient;
        }
        if (attestationResponseHandler == null) {
            throw new IllegalArgumentException("attestationResponseHandler can not be null");
        } else {
            this.attestationResponseHandler = attestationResponseHandler;
        }

        this.appVersionHeader = attestationResponseHandler.getAppVersionHeader();
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        return this.internalDownload(path);
    }

    protected String getJWT() {
        return this.getAttestationResponseHandler().getCoreJWT();
    }

    private InputStream internalDownload(String path) throws CloudStorageException {
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
            throw new CloudStorageException("download error: " + e.getMessage(), e);
        }
    }

    private InputStream readContentFromLocalFileSystem(String path, Proxy proxy) throws IOException {
        return (proxy == null ? new URL(path).openConnection() : new URL(path).openConnection(proxy)).getInputStream();
    }

    private InputStream getWithAttest(String path) throws IOException, AttestationResponseHandlerException {
        if (!attestationResponseHandler.attested()) {
            attestationResponseHandler.attest();
        }

        String attestationToken = attestationResponseHandler.getAttestationToken();

        HttpResponse<String> httpResponse;
        httpResponse = sendHttpRequest(path, attestationToken);

        return Utils.convertHttpResponseToInputStream(httpResponse);
    }

    private HttpResponse<String> sendHttpRequest(String path, String attestationToken) throws IOException {
        URI uri = URI.create(path);

        HashMap<String, String> headers = new HashMap<>();
        headers.put(Const.Http.AppVersionHeader, this.appVersionHeader);
        if (this.userToken != null && !this.userToken.isBlank()) {
            headers.put("Authorization", "Bearer " + this.userToken);
        }
        if (attestationToken != null && !attestationToken.isBlank()) {
            headers.put(Const.Attestation.AttestationTokenHeader, attestationToken);
        }

        String jwtToken = this.getJWT();
        if (jwtToken != null && !jwtToken.isBlank()) {
            headers.put(Const.Attestation.AttestationJWTHeader, jwtToken);
        } else {
            LOGGER.warn("getJWT returned an empty or null string for the JWT");
        }

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.get(path, headers);
        } catch (IOException e) {
            LOGGER.error("Failed to send request to host: " + uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + " with error: ", e);
            throw e;
        }
        return httpResponse;
    }

    protected AttestationResponseHandler getAttestationResponseHandler() {
        return attestationResponseHandler;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public void setAllowContentFromLocalFileSystem(boolean allowContentFromLocalFileSystem) {
        this.allowContentFromLocalFileSystem = allowContentFromLocalFileSystem;
    }
}
