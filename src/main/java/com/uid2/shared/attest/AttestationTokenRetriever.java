package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.ApplicationVersion;
import com.uid2.shared.Const;
import com.uid2.shared.Utils;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AttestationTokenRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidCoreClient.class);
    private final IAttestationProvider attestationProvider;
    private final ApplicationVersion appVersion;
    private AtomicReference<String> attestationToken;
    private final String userToken;
    private AtomicReference<Handler<Integer>> responseWatcher;
    private final String attestationEndpoint;
    private final String appVersionHeader;
    private final Proxy proxy;
    private final boolean enforceHttps;
    private boolean allowContentFromLocalFileSystem = false;
    private ScheduledThreadPoolExecutor executor;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;

    public AttestationTokenRetriever(String attestationEndpoint, String userToken, ApplicationVersion appVersion, Proxy proxy,
                                     IAttestationProvider attestationProvider, boolean enforceHttps,
                                     boolean allowContentFromLocalFileSystem, AtomicReference<Handler<Integer>> responseWatcher) {
        this.attestationEndpoint = attestationEndpoint;
        this.userToken = userToken;
        this.appVersion = appVersion;
        this.proxy = proxy;
        this.attestationProvider = attestationProvider;
        this.attestationToken = new AtomicReference<>(null);
        this.enforceHttps = enforceHttps;
        this.allowContentFromLocalFileSystem = allowContentFromLocalFileSystem;
        this.responseWatcher = responseWatcher;

        String appVersionHeader = appVersion.getAppName() + "=" + appVersion.getAppVersion();
        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet())
            appVersionHeader += ";" + kv.getKey() + "=" + kv.getValue();
        this.appVersionHeader = appVersionHeader;

        // Create the ScheduledThreadPoolExecutor instance with the desired number of threads
        int numberOfThreads = 1;
        this.executor = new ScheduledThreadPoolExecutor(numberOfThreads);
    }

    private void attestationExpirationCheck(){
        Instant currentTime = Instant.now();
        Instant tenMinutesBeforeExpire = attestationTokenExpiresAt.minus(Duration.ofMinutes(10));

        if (currentTime.isAfter(tenMinutesBeforeExpire)) {
            LOGGER.info("Attestation token is 10 mins from the expiry timestamp %s. Re-attest...", attestationTokenExpiresAt);
            try {
                attestInternal();
            }
            catch (UidCoreClientException | IOException e) {
                notifyResponseStatusWatcher(401);
                LOGGER.info("Re-attest failed: ", e.getMessage());
            }
        }
    }

    private void scheduleAttestationExpirationCheck() {
        // Schedule the task to run every 9 minutes
        executor.scheduleAtFixedRate(this::attestationExpirationCheck, 0, TimeUnit.MINUTES.toMillis(9), TimeUnit.MILLISECONDS);
    }

    private void stopAttestationExpirationCheck() {
        executor.shutdown();
    }

    public void attest() throws IOException, UidCoreClientException {
        attestInternal();
    }

    private boolean attested() {
        return this.attestationToken.get() != null;
    }

    public boolean attestIfRequired(HttpURLConnection conn) throws IOException, UidCoreClientException {
        boolean attested = false;
        int statusCode = conn.getResponseCode();
        if (statusCode == 401) {
            LOGGER.info("Initial response from UID2 Core returned 401, performing attestation");
            attested = true;
            try {
                attestInternal();
            }
            catch (UidCoreClientException | IOException e) {
                notifyResponseStatusWatcher(statusCode);
                throw e;
            }
        } else {
            notifyResponseStatusWatcher(statusCode);
        }
        return attested;
    }

    private void attestInternal() throws IOException, UidCoreClientException {
        try {
            JsonObject requestJson = new JsonObject();
            KeyPair keyPair = generateKeyPair();
            byte[] publicKey = keyPair.getPublic().getEncoded();
            requestJson.put("attestation_request", Base64.getEncoder().encodeToString(attestationProvider.getAttestationRequest(publicKey)));
            requestJson.put("public_key", Base64.getEncoder().encodeToString(publicKey));
            requestJson.put("application_name", appVersion.getAppName());
            requestJson.put("application_version", appVersion.getAppVersion());
            JsonObject components = new JsonObject();
            for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet()) {
                components.put(kv.getKey(), kv.getValue());
            }
            requestJson.put("components", components);

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

            String atoken = getAttestationToken(responseJson);
            if (atoken == null) {
                throw new UidCoreClientException(statusCode, "response json does not contain body.attestation_token");
            }
            String expiresAt = getAttestationTokenExpiresAt(responseJson);
            if (expiresAt == null) {
                throw new UidCoreClientException(statusCode, "response json does not contain body.expiresAt");
            }

            atoken = new String(decrypt(Base64.getDecoder().decode(atoken), keyPair.getPrivate()), StandardCharsets.UTF_8);
            LOGGER.info("Attestation successful. Attestation token received.");
            this.attestationToken.set(atoken);
            this.attestationTokenExpiresAt = Instant.parse(expiresAt);

            scheduleAttestationExpirationCheck();
        } catch (AttestationException ae) {
            throw new UidCoreClientException(ae);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new UidCoreClientException(e);
        }
    }

    private static String getAttestationToken(JsonObject responseJson) {
        final JsonObject body = responseJson.getJsonObject("body");
        if(body == null) return null;
        return body.getString("attestation_token");
    }

    private static String getAttestationTokenExpiresAt(JsonObject responseJson) {
        final JsonObject body = responseJson.getJsonObject("body");
        if (body == null) return null;
        return body.getString("expiresAt");
    }

    private static boolean isFailed(JsonObject responseJson) {
        return responseJson.getString("status") == null || !responseJson.getString("status").equals("success");
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

    public void notifyResponseStatusWatcher(int statusCode) {
        Handler<Integer> w = this.responseWatcher.get();
        if (w != null)
            w.handle(statusCode);
    }

    private URLConnection sendGet(String url) throws IOException {
        final URLConnection conn = openConnection(url, "GET");
        return conn;
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

        final String atoken = this.attestationToken.get();
        if(atoken != null && atoken.length() > 0) {
            connection.setRequestProperty("Attestation-Token", atoken);
        }

        return connection;
    }
}
