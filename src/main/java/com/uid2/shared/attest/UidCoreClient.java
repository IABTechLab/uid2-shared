// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import com.uid2.shared.cloud.*;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class UidCoreClient implements IUidCoreClient, ICloudStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClient.class);
    private final IAttestationProvider attestationProvider;
    private final ICloudStorage contentStorage;
    private final Proxy proxy;
    private final String attestationEndpoint;
    private final String userToken;
    private final ApplicationVersion appVersion;
    private final String appVersionHeader;
    private AtomicReference<String> attestationToken;
    private final boolean enforceHttps;
    private static boolean useSecureParameters = true;
    private boolean allowContentFromLocalFileSystem = false;
    private AtomicReference<Handler<Integer>> responseWatcher;

    public static UidCoreClient createNoAttest(String attestationEndpoint, String userToken, ApplicationVersion appVersion, boolean enforceHttps) {
        return new UidCoreClient(attestationEndpoint, userToken, appVersion, CloudUtils.defaultProxy, new NoAttestationProvider(), enforceHttps);
    }

    public UidCoreClient(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                         IAttestationProvider attestationProvider, boolean enforceHttps) {
        this.attestationEndpoint = attestationEndpoint;
        this.proxy = proxy;
        this.userToken = userToken;
        this.appVersion = appVersion;
        this.attestationProvider = attestationProvider;
        this.contentStorage = new PreSignedURLStorage(proxy);
        this.attestationToken = new AtomicReference<>(null);
        this.enforceHttps = enforceHttps;
        this.responseWatcher = new AtomicReference<Handler<Integer>>(null);

        String appVersionHeader = appVersion.getAppName() + "=" + appVersion.getAppVersion();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet())
            appVersionHeader += ";" + kv.getKey() + "=" + kv.getValue();
        this.appVersionHeader = appVersionHeader;
    }

    @Override
    public ICloudStorage getContentStorage() {
        return this.contentStorage;
    }

    /**
     * HTTP Get with automatic attestation on first 401 Unauthorized response
     * @param url API endpoint
     * @return InputStream from HTTP response
     * @throws IOException on HTTP failures
     * @throws UidCoreClientException on attestation failures
     */
    private InputStream getWithAttest(String url) throws IOException, UidCoreClientException {
        if (!attested()) {
            try {
                attest();
            } catch (UidCoreClientException | IOException e) {
                LOGGER.warn("Initial attestation failed: " + e.getMessage());
                notifyResponseStatusWatcher(401);
                throw e;
            }
        }

        HttpURLConnection conn = (HttpURLConnection) openConnection(url, "GET");
        int statusCode = conn.getResponseCode();
        if (statusCode == 401) {
            LOGGER.info("First response from UID2 Core returned 401. We might hold an expired attestation token. Performing attestation.");
            try {
                attest();
            } catch (UidCoreClientException | IOException e) {
                LOGGER.warn("Attestation failed: " + e.getMessage());
                notifyResponseStatusWatcher(401);
                throw e;
            }

            // Note: re-initiate `conn` and `statusCode`
            conn = (HttpURLConnection) openConnection(url, "GET");
            statusCode = conn.getResponseCode();
        }

        notifyResponseStatusWatcher(statusCode);
        return conn.getInputStream();
    }

    public void setAllowContentFromLocalFileSystem(boolean allow) {
        allowContentFromLocalFileSystem = allow;
    }

    // open connection with auth & attestation headers attached
    private URLConnection openConnection(String serviceEndpoint, String httpMethod) throws IOException {
        final URLConnection urlConnection = (proxy == null ? new URL(serviceEndpoint).openConnection() : new URL(serviceEndpoint).openConnection(proxy));

        if(enforceHttps && !(urlConnection instanceof HttpsURLConnection)) {
            throw new IOException("UidCoreClient requires HTTPS connection");
        }

        if (allowContentFromLocalFileSystem && serviceEndpoint.startsWith("file:/tmp/uid2")) {
            // returns `file:/tmp/uid2` urlConnection directly
            return urlConnection;
        }

        final HttpURLConnection connection = (HttpURLConnection) urlConnection;
        connection.setRequestMethod(httpMethod);
        connection.setRequestProperty(Const.Http.AppVersionHeader, appVersionHeader);

        if(this.userToken != null && this.userToken.length() > 0) {
            connection.setRequestProperty("Authorization", "Bearer " + this.userToken);
        }

        final String attestationToken = this.attestationToken.get();
        if(attestationToken != null && attestationToken.length() > 0) {
            connection.setRequestProperty("Attestation-Token", attestationToken);
        }

        return connection;
    }

    //region attestation methods

    private boolean attested() {
        return this.attestationToken.get() != null;
    }

    /**
     * Attest with Core service
     * @throws IOException HTTP failures
     * @throws UidCoreClientException Attestation Failures
     */
    private void attest() throws IOException, UidCoreClientException {
        try {
            KeyPair keyPair = generateKeyPair();
            JsonObject requestJson = createAttestationRequest(keyPair.getPublic());

            HttpURLConnection connection = (HttpURLConnection) openConnection(attestationEndpoint, "POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream request = connection.getOutputStream()) {
                request.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            notifyResponseStatusWatcher(statusCode);

            if (statusCode < 200 || statusCode >= 300) {
                LOGGER.warn("attestation failed with UID2 Core returning statusCode=" + statusCode);
                throw new UidCoreClientException(statusCode, "unexpected status code from uid core service");
            }

            String response = Utils.readToEnd(connection.getInputStream());
            JsonObject responseJson = (JsonObject) Json.decodeValue(response);
            if (isFailed(responseJson)) {
                throw new UidCoreClientException(statusCode, "response did not return a successful status");
            }

            String attestationToken = getAttestationToken(responseJson);
            if (attestationToken == null) {
                throw new UidCoreClientException(statusCode, "response json does not contain body.attestation_token");
            }

            attestationToken = new String(decrypt(Base64.getDecoder().decode(attestationToken), keyPair.getPrivate()), StandardCharsets.UTF_8);
            LOGGER.info("Attestation successful. Attestation token received.");
            this.attestationToken.set(attestationToken);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new UidCoreClientException(e);
        }
    }

    private JsonObject createAttestationRequest(PublicKey publicKey) throws AttestationException {
        JsonObject requestJson = new JsonObject();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        requestJson.put("attestation_request", Base64.getEncoder().encodeToString(attestationProvider.getAttestationRequest(publicKeyEncoded)));
        requestJson.put("public_key", Base64.getEncoder().encodeToString(publicKeyEncoded));
        requestJson.put("application_name", appVersion.getAppName());
        requestJson.put("application_version", appVersion.getAppVersion());
        JsonObject components = new JsonObject();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet()) {
            components.put(kv.getKey(), kv.getValue());
        }
        requestJson.put("components", components);
        return requestJson;
    }

    private static boolean isFailed(JsonObject responseJson) {
        return responseJson.getString("status") == null || !responseJson.getString("status").equals("success");
    }

    private static String getAttestationToken(JsonObject responseJson) {
        final JsonObject body = responseJson.getJsonObject("body");
        if(body == null) return null;
        return body.getString("attestation_token");
    }

    private static byte[] decrypt(byte[] payload, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(Const.Name.AsymetricEncryptionCipherClass);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(payload);
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(Const.Name.AsymetricEncryptionKeyClass);
        gen.initialize(2048, new SecureRandom());
        return gen.generateKeyPair();
    }

    //endregion attestation methods

    @Override
    public void upload(String s, String s1) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::upload method is not supported");
    }

    @Override
    public void upload(InputStream s, String s1) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::upload method is not supported");
    }

    @Override
    public InputStream download(String path) throws CloudStorageException {
        try {
            return getWithAttest(path);
        } catch (Exception e) {
            throw new CloudStorageException("download " + path + " error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::delete method is not supported");
    }

    @Override
    public void delete(Collection<String> cloudPath) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::delete method is not supported");
    }

    @Override
    public List<String> list(String s) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::list method is not supported");
    }

    @Override
    public URL preSignUrl(String s) throws CloudStorageException {
        throw new UnsupportedOperationException("UidCoreClient::preSignUrl method is not supported");
    }

    @Override
    public void setPreSignedUrlExpiry(long expiry) {
        throw new UnsupportedOperationException("UidCoreClient::preSignUrl method is not supported");
    }

    @Override
    public String mask(String cloudPath) {
        throw new UnsupportedOperationException("UidCoreClient::mask method is not supported");
    }

    public void setResponseStatusWatcher(Handler<Integer> watcher) {
        this.responseWatcher.set(watcher);
    }

    public void notifyResponseStatusWatcher(int statusCode) {
        Handler<Integer> w = this.responseWatcher.get();
        if (w != null)
            w.handle(statusCode);
    }
}
