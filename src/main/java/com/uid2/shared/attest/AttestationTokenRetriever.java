package com.uid2.shared.attest;

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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AttestationTokenRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttestationTokenRetriever.class);
    private final IAttestationProvider attestationProvider;
    private final String clientApiToken;
    private final ApplicationVersion appVersion;
    private final AtomicReference<String> attestationToken;
    private final AtomicReference<String> optOutJwt;
    private final AtomicReference<String> coreJwt;
    private final Handler<Integer> responseWatcher;
    private final String attestationEndpoint;
    private final HttpClient httpClient;
    private final IClock clock;
    private final Vertx vertx;
    private boolean isExpiryCheckScheduled;
    private boolean isAttesting;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;
    private final Lock lock;
    private final AttestationTokenDecryptor attestationTokenDecryptor;
    private final String appVersionHeader;

    public AttestationTokenRetriever(Vertx vertx,
                                     String attestationEndpoint,
                                     String clientApiToken,
                                     ApplicationVersion appVersion,
                                     IAttestationProvider attestationProvider,
                                     Handler<Integer> responseWatcher) {
        this(vertx, attestationEndpoint, clientApiToken, appVersion, attestationProvider, responseWatcher, new InstantClock(), null, null);
    }
    public AttestationTokenRetriever(Vertx vertx,
                                     String attestationEndpoint,
                                     String clientApiToken,
                                     ApplicationVersion appVersion,
                                     IAttestationProvider attestationProvider,
                                     Handler<Integer> responseWatcher,
                                     IClock clock,
                                     HttpClient httpClient,
                                     AttestationTokenDecryptor attestationTokenDecryptor) {
        this.vertx = vertx;
        this.attestationEndpoint = attestationEndpoint;
        this.clientApiToken = clientApiToken;
        this.appVersion = appVersion;
        this.attestationProvider = attestationProvider;
        this.attestationToken = new AtomicReference<>(null);
        this.optOutJwt = new AtomicReference<>(null);
        this.coreJwt = new AtomicReference<>(null);
        this.responseWatcher = responseWatcher;
        this.clock = clock;
        this.lock = new ReentrantLock();
        this.isExpiryCheckScheduled = false;
        this.isAttesting = false;
        if (httpClient == null) {
            this.httpClient = HttpClient.newHttpClient();
        } else {
            this.httpClient = httpClient;
        }
        this.attestationTokenDecryptor = Objects.requireNonNullElseGet(attestationTokenDecryptor, AttestationTokenDecryptor::new);

        StringBuilder builder = new StringBuilder();
        builder.append(appVersion.getAppName())
                .append("=")
                .append(appVersion.getAppVersion());

        for (Map.Entry<String, String> kv : appVersion.getComponentVersions().entrySet()) {
            builder.append(";")
                    .append(kv.getKey())
                    .append("=")
                    .append(kv.getValue());
        }
        this.appVersionHeader = builder.toString();

    }

    private void attestationExpirationCheck(long timerId) {
        // This check is to avoid the attest() function takes longer than 60sec and get called again from this method while attesting.
        if (this.isAttesting) {
            LOGGER.warn("In the process of attesting. Skip re-attest.");
        } else {
            Instant currentTime = clock.now();
            Instant tenMinutesBeforeExpire = attestationTokenExpiresAt.minusSeconds(600);

            if (currentTime.isAfter(tenMinutesBeforeExpire)) {
                LOGGER.info("Attestation token is 10 mins from the expiry timestamp {}. Re-attest...", attestationTokenExpiresAt);
                try {
                    this.isAttesting = true;
                    attest();
                } catch (AttestationTokenRetrieverException | IOException e) {
                    notifyResponseStatusWatcher(401);
                    LOGGER.info("Re-attest failed: ", e);
                } finally {
                    this.isAttesting = false;
                }
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
                    .setHeader("Authorization", "Bearer " + this.clientApiToken)
                    .setHeader(Const.Http.AppVersionHeader, this.appVersionHeader)
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

            JsonObject innerBody = responseJson.getJsonObject("body");
            if (innerBody == null) {
                throw new AttestationTokenRetrieverException(statusCode, "response did not contain a body object");
            }

            String atoken = getAttestationToken(innerBody);
            if (atoken == null) {
                throw new AttestationTokenRetrieverException(statusCode, "response json does not contain body.attestation_token");
            }
            String expiresAt = getAttestationTokenExpiresAt(innerBody);
            if (expiresAt == null) {
                throw new AttestationTokenRetrieverException(statusCode, "response json does not contain body.expiresAt");
            }

            atoken = new String(attestationTokenDecryptor.decrypt(Base64.getDecoder().decode(atoken), keyPair.getPrivate()), StandardCharsets.UTF_8);
            LOGGER.info("Attestation successful. Attestation token received.");
            setAttestationToken(atoken);
            setAttestationTokenExpiresAt(expiresAt);
            setOptoutJWTFromResponse(innerBody);
            setCoreJWTFromResponse(innerBody);

            scheduleAttestationExpirationCheck();
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

    public String getOptOutJWT() {
        return this.optOutJwt.get();
    }

    public String getCoreJWT() {
        return this.coreJwt.get();
    }

    public String getAppVersionHeader() {
        return this.appVersionHeader;
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

    private void setOptoutJWTFromResponse(JsonObject responseBody) {
        String jwt = responseBody.getString("attestation_jwt_optout");
        if (jwt == null) {
            LOGGER.info("Optout JWT not received");
        } else {
            LOGGER.info("Optout JWT received");
            this.optOutJwt.set(jwt);
        }
    }

    private void setCoreJWTFromResponse(JsonObject responseBody) {
        String jwt = responseBody.getString("attestation_jwt_core");
        if (jwt == null) {
            LOGGER.info("Core JWT not received");
        } else {
            LOGGER.info("Core JWT received");
            this.coreJwt.set(jwt);
        }
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
        return this.attestationToken.get() != null && this.clock.now().isBefore(this.attestationTokenExpiresAt);
    }
}
