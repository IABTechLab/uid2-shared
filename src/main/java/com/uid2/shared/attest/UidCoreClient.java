package com.uid2.shared.attest;

import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.audit.Audit;
import com.uid2.shared.audit.UidInstanceIdProvider;
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
    private final UidInstanceIdProvider uidInstanceIdProvider;
    private String userToken;
    private final String appVersionHeader;
    private boolean allowContentFromLocalFileSystem = false;
    private boolean encryptionEnabled;
    private final AttestationResponseHandler attestationResponseHandler;


    public static UidCoreClient createNoAttest(String userToken, AttestationResponseHandler attestationResponseHandler, UidInstanceIdProvider uidInstanceIdProvider) {
        return new UidCoreClient(userToken, CloudUtils.defaultProxy, attestationResponseHandler, null, false, uidInstanceIdProvider);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         AttestationResponseHandler attestationResponseHandler,
                         boolean encryptionEnabled,
                         UidInstanceIdProvider uidInstanceIdProvider) {
        this(userToken, proxy, attestationResponseHandler, null, encryptionEnabled, uidInstanceIdProvider);
    }

    public UidCoreClient(String userToken,
                         Proxy proxy,
                         AttestationResponseHandler attestationResponseHandler,
                         URLConnectionHttpClient httpClient,
                         boolean encryptionEnabled,
                         UidInstanceIdProvider uidInstanceIdProvider) {
        this.encryptionEnabled = encryptionEnabled;
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
        this.uidInstanceIdProvider = uidInstanceIdProvider;
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
        } catch (CloudStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudStorageException(
                "E12: Data Download Failure - exception: " + e.getClass().getSimpleName() + 
                ". Please visit UID2 guides for troubleshooting", e);
        }
    }

    private InputStream readContentFromLocalFileSystem(String path, Proxy proxy) throws IOException {
        return (proxy == null ? new URL(path).openConnection() : new URL(path).openConnection(proxy)).getInputStream();
    }

    private InputStream getWithAttest(String path) throws IOException, AttestationResponseHandlerException, CloudStorageException {
        if (!attestationResponseHandler.attested()) {
            attestationResponseHandler.attest();
        }

        String attestationToken = attestationResponseHandler.getAttestationToken();

        HttpResponse<String> httpResponse;
        httpResponse = sendHttpRequest(path, attestationToken);
        if (httpResponse.statusCode() != 200) {
            throw new CloudStorageException(String.format(
                "E12: Data Download Failure - HTTP response code %d. Please visit UID2 guides for troubleshooting", 
                httpResponse.statusCode()));
        }
        return Utils.convertHttpResponseToInputStream(httpResponse);
    }

    private HttpResponse<String> sendHttpRequest(String path, String attestationToken) throws IOException {
        URI uri = URI.create(path);

        HashMap<String, String> headers = new HashMap<>();
        headers.put(Const.Http.AppVersionHeader, this.appVersionHeader);
        if (this.encryptionEnabled)
            headers.put("Encrypted", String.valueOf(this.encryptionEnabled));
        if (this.userToken != null && !this.userToken.isBlank()) {
            headers.put("Authorization", "Bearer " + this.userToken);
        }
        if (attestationToken != null && !attestationToken.isBlank()) {
            headers.put(Const.Attestation.AttestationTokenHeader, attestationToken);
        }
        headers.put(Audit.UID_INSTANCE_ID_HEADER, this.uidInstanceIdProvider.getInstanceId());

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
