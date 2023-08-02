package com.uid2.shared.attest;

import com.uid2.enclave.AttestationException;
import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.*;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AttestationTokenRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttestationTokenRetriever.class);
    private final IAttestationProvider attestationProvider;
    private final ApplicationVersion appVersion;
    private AtomicReference<String> attestationToken;
    private Handler<Integer> responseWatcher;
    private final String attestationEndpoint;
    private final HttpClient httpClient;
    private final IClock clock;
    private Vertx vertx;
    private boolean isExpiryCheckScheduled;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;
    private final Lock lock;
    private final AttestationTokenDecryptor attestationTokenDecryptor;

    public AttestationTokenRetriever(String attestationEndpoint, ApplicationVersion appVersion, IAttestationProvider attestationProvider,
                                     Handler<Integer> responseWatcher, IClock clock, HttpClient httpClient,
                                     AttestationTokenDecryptor attestationTokenDecryptor) throws IOException {
        this.attestationEndpoint = attestationEndpoint;
        this.appVersion = appVersion;
        this.attestationProvider = attestationProvider;
        this.attestationToken = new AtomicReference<>(null);
        this.responseWatcher = responseWatcher;
        this.clock = clock;
        this.lock = new ReentrantLock();
        this.isExpiryCheckScheduled = false;
        if (httpClient == null) {
            this.httpClient = HttpClient.newHttpClient();
        } else {
            this.httpClient = httpClient;
        }
        if (attestationTokenDecryptor == null) {
            this.attestationTokenDecryptor = new AttestationTokenDecryptor();
        } else {
            this.attestationTokenDecryptor = attestationTokenDecryptor;
        }
    }

    private void attestationExpirationCheck(long timerId) {
        Instant currentTime = clock.now();
        Instant tenMinutesBeforeExpire = attestationTokenExpiresAt.minusSeconds(600);

        if (currentTime.isAfter(tenMinutesBeforeExpire)) {
            LOGGER.info("Attestation token is 10 mins from the expiry timestamp %s. Re-attest...", attestationTokenExpiresAt);
            try {
                attest();
            } catch (AttestationTokenRetrieverException | IOException e) {
                notifyResponseStatusWatcher(401);
                LOGGER.info("Re-attest failed: ", e.getMessage());
            }
        }
    }

    private void scheduleAttestationExpirationCheck() {
        if (!this.isExpiryCheckScheduled) {
            // Schedule the task to run every minute
            this.vertx.setPeriodic(0, 60000, this::attestationExpirationCheck);
            this.isExpiryCheckScheduled = true;
        }
    }

    public void attest() throws IOException, AttestationTokenRetrieverException {
        try {
            KeyPair keyPair = generateKeyPair();
            byte[] publicKey = keyPair.getPublic().getEncoded();
            JsonObject requestJson = JsonObject.of(
                    "attestation_request", Base64.getEncoder().encodeToString(attestationProvider.getAttestationRequest(publicKey)),
                    "public_key", Base64.getEncoder().encodeToString(publicKey),
                    "application_name", appVersion.getAppName(),
                    "application_version", appVersion.getAppVersion()
            );
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

            atoken = new String(attestationTokenDecryptor.decrypt(Base64.getDecoder().decode(atoken), keyPair.getPrivate()), StandardCharsets.UTF_8);
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

    public String getAttestationToken() {
        return this.attestationToken.get();
    }

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

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(Const.Name.AsymetricEncryptionKeyClass);
        gen.initialize(2048, new SecureRandom());
        return gen.generateKeyPair();
    }

    private void notifyResponseStatusWatcher(int statusCode) {
        this.lock.lock();
        try {
            if (this.responseWatcher != null)
                this.responseWatcher.handle(statusCode);
        } finally {
            lock.unlock();
        }
    }

    public boolean attested() {
        if (this.attestationToken.get() != null && this.clock.now().isBefore(this.attestationTokenExpiresAt)) {
            return true;
        }
        return false;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }
}
