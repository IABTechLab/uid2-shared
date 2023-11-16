package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.*;
import com.uid2.shared.util.URLConnectionHttpClient;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.Pair;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final Handler<Pair<Integer, String>> responseWatcher;
    private final String attestationEndpoint;
    private final IClock clock;
    private final Vertx vertx;
    private final URLConnectionHttpClient httpClient;
    private boolean isExpiryCheckScheduled;
    private AtomicBoolean isAttesting;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;
    private final Lock lock;
    private final AttestationTokenDecryptor attestationTokenDecryptor;
    private final String appVersionHeader;
    private final int attestCheckMilliseconds;

    public AttestationTokenRetriever(Vertx vertx,
                                     String attestationEndpoint,
                                     String clientApiToken,
                                     ApplicationVersion appVersion,
                                     IAttestationProvider attestationProvider,
                                     Handler<Pair<Integer, String>> responseWatcher,
                                     Proxy proxy) {
        this(vertx, attestationEndpoint, clientApiToken, appVersion, attestationProvider, responseWatcher, proxy, new InstantClock(), null, null, 60000);
    }
    public AttestationTokenRetriever(Vertx vertx,
                                     String attestationEndpoint,
                                     String clientApiToken,
                                     ApplicationVersion appVersion,
                                     IAttestationProvider attestationProvider,
                                     Handler<Pair<Integer, String>> responseWatcher,
                                     Proxy proxy,
                                     IClock clock,
                                     URLConnectionHttpClient httpClient,
                                     AttestationTokenDecryptor attestationTokenDecryptor,
                                     int attestCheckMilliseconds) {
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
        this.isAttesting = new AtomicBoolean(false);
        this.attestCheckMilliseconds = attestCheckMilliseconds;
        if (httpClient == null) {
            this.httpClient = new URLConnectionHttpClient(proxy);
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
        if (!this.isAttesting.compareAndSet(false, true)) {
            LOGGER.warn("In the process of attesting. Skip re-attest.");
            return;
        }

        try {
            Instant currentTime = clock.now();
            Instant tenMinutesBeforeExpire = attestationTokenExpiresAt.minusSeconds(600);
            if (!currentTime.isAfter(tenMinutesBeforeExpire)) {
                return;
            }

            LOGGER.info("Attestation token is 10 mins from the expiry timestamp {}. Re-attest...", attestationTokenExpiresAt);

            if (!attestationProvider.isReady()) {
                LOGGER.warn("Attestation provider is not ready. Skip re-attest.");
                return;
            }

            attest();
        } catch (AttestationTokenRetrieverException e) {
            notifyResponseWatcher(401, e.getMessage());
            LOGGER.info("Re-attest failed: ", e);
        } catch (IOException e){
            notifyResponseWatcher(500, e.getMessage());
            LOGGER.info("Re-attest failed: ", e);
        } finally {
            this.isAttesting.set(false);
        }
    }

    private void scheduleAttestationExpirationCheck() {
        if (!this.isExpiryCheckScheduled) {
            // Schedule the task to run every minute
            this.vertx.setPeriodic(0, attestCheckMilliseconds, this::attestationExpirationCheck);
            this.isExpiryCheckScheduled = true;
        }
    }

    public void attest() throws IOException, AttestationTokenRetrieverException {
        if (!attestationProvider.isReady()) {
            throw new AttestationTokenRetrieverException("attestation provider is not ready");
        }

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

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + this.clientApiToken);
            headers.put(Const.Http.AppVersionHeader, this.appVersionHeader);

            HttpResponse<String> response = httpClient.post(attestationEndpoint, requestJson.toString(), headers);

            int statusCode = response.statusCode();
            String responseBody = response.body();
            notifyResponseWatcher(statusCode, responseBody);

            if (statusCode < 200 || statusCode >= 300) {
                LOGGER.warn("attestation failed with UID2 Core returning statusCode=" + statusCode);
                throw new AttestationTokenRetrieverException(statusCode, "unexpected status code from uid core service");
            }

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

    private void notifyResponseWatcher(int statusCode, String responseBody) {
        this.lock.lock();
        try {
            if (this.responseWatcher != null)
                this.responseWatcher.handle(Pair.of(statusCode, responseBody));
        } finally {
            lock.unlock();
        }
    }

    public boolean attested() {
        return this.attestationToken.get() != null && this.clock.now().isBefore(this.attestationTokenExpiresAt);
    }
}
