package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.*;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
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
    private AtomicReference<Handler<Integer>> responseWatcher;
    private final String attestationEndpoint;
    private HttpClient httpClient;
    private final IClock clock;
    private ScheduledThreadPoolExecutor executor;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;

    public AttestationTokenRetriever(String attestationEndpoint, ApplicationVersion appVersion, Proxy proxy,
                                     IAttestationProvider attestationProvider, AtomicReference<Handler<Integer>> responseWatcher,
                                     IClock clock) throws IOException {
        this.attestationEndpoint = attestationEndpoint;
        this.appVersion = appVersion;
        this.attestationProvider = attestationProvider;
        this.attestationToken = new AtomicReference<>(null);
        this.responseWatcher = responseWatcher;
        this.clock = clock;
        this.httpClient = HttpClient.newHttpClient();

        // Create the ScheduledThreadPoolExecutor instance with the desired number of threads
        int numberOfThreads = 1;
        this.executor = new ScheduledThreadPoolExecutor(numberOfThreads);
    }

    private void attestationExpirationCheck(){
        Instant currentTime = clock.now();
        Instant tenMinutesBeforeExpire = attestationTokenExpiresAt.minusSeconds(600);

        stopAttestationExpirationCheck();

        if (currentTime.isAfter(tenMinutesBeforeExpire)) {
            LOGGER.info("Attestation token is 10 mins from the expiry timestamp %s. Re-attest...", attestationTokenExpiresAt);
            try {
                attestInternal();
            }
            catch (AttestationTokenRetrieverException | IOException e) {
                notifyResponseStatusWatcher(401);
                LOGGER.info("Re-attest failed: ", e.getMessage());
            }
        }
    }

    private void scheduleAttestationExpirationCheck() {
        // Schedule the task to run every minute
        executor.scheduleAtFixedRate(this::attestationExpirationCheck, 0, TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS);
    }

    private void stopAttestationExpirationCheck() {
        executor.shutdown();
    }

    public void attestInternal() throws IOException, AttestationTokenRetrieverException {
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

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .setHeader("Content-Type", "application/json")
                    .uri(URI.create(attestationEndpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString(), StandardCharsets.UTF_8))
                    .build();

//            connection.setDoOutput(true);
//            try (OutputStream request = connection.getOutputStream()) {
//                request.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
//            }

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            notifyResponseStatusWatcher(statusCode);

            if (statusCode < 200 || statusCode >= 300) {
                LOGGER.warn("attestation failed with UID2 Core returning statusCode=" + statusCode);
                throw new AttestationTokenRetrieverException(statusCode, "unexpected status code from uid core service");
            }

            String responseBody = response.body();
            JsonObject responseJson = (JsonObject) Json.decodeValue(responseBody);
            if (isFailed(responseJson)) {
                throw new AttestationTokenRetrieverException(statusCode, "response did not return a successful status");
            }

            String atoken = getAttestationToken(responseJson);
            if (atoken == null) {
                throw new AttestationTokenRetrieverException(statusCode, "response json does not contain body.attestation_token");
            }
            String expiresAt = getAttestationTokenExpiresAt(responseJson);
            if (expiresAt == null) {
                throw new AttestationTokenRetrieverException(statusCode, "response json does not contain body.expiresAt");
            }

            atoken = new String(decrypt(Base64.getDecoder().decode(atoken), keyPair.getPrivate()), StandardCharsets.UTF_8);
            LOGGER.info("Attestation successful. Attestation token received.");
            setAttestationToken(atoken);
            setAttestationTokenExpiresAt(expiresAt);

            scheduleAttestationExpirationCheck();
        } catch (AttestationException ae) {
            throw new AttestationTokenRetrieverException(ae);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new AttestationTokenRetrieverException(e);
        }
    }

    public String getAttestationToken() { return this.attestationToken.get(); }
    private void setAttestationToken(String atoken) {
        this.attestationToken.set(atoken);
    }
    private void setAttestationTokenExpiresAt(String expiresAt) {
        this.attestationTokenExpiresAt = Instant.parse(expiresAt);
    }

    private static String getAttestationToken(JsonObject responseBody) {
        return responseBody.getString("attestation_token");
    }

    private static String getAttestationTokenExpiresAt(JsonObject responseBody) {
        return responseBody.getString("expiresAt");
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

    private void notifyResponseStatusWatcher(int statusCode) {
        Handler<Integer> w = this.responseWatcher.get();
        if (w != null)
            w.handle(statusCode);
    }

    public boolean attested() {
        if (this.attestationToken.get() != null && this.clock.now().isBefore(this.attestationTokenExpiresAt)) return true;
        return false;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
