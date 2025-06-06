package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;
import com.uid2.shared.*;
import com.uid2.shared.audit.Audit;
import com.uid2.shared.audit.UidInstanceIdProvider;
import com.uid2.shared.util.URLConnectionHttpClient;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.Pair;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AttestationResponseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttestationResponseHandler.class);

    private final IAttestationProvider attestationProvider;
    private final String clientApiToken;
    private final String operatorType;
    private final ApplicationVersion appVersion;
    private final AtomicReference<String> attestationToken;
    private final AtomicReference<String> optOutJwt;
    private final AtomicReference<String> coreJwt;
    private final Handler<Pair<AttestationResponseCode, String>> responseWatcher;
    private final String attestationEndpoint;
    private final byte[] encodedAttestationEndpoint;
    private final IClock clock;
    private final Vertx vertx;
    private final URLConnectionHttpClient httpClient;
    private final UidInstanceIdProvider uidInstanceIdProvider;
    private boolean isExpiryCheckScheduled;
    private AtomicBoolean isAttesting;
    // Set this to be Instant.MAX so that if it's not set it won't trigger the re-attest
    private Instant attestationTokenExpiresAt = Instant.MAX;
    private final Lock lock;
    private final AttestationTokenDecryptor attestationTokenDecryptor;
    @Getter
    private final String appVersionHeader;
    private final int attestCheckMilliseconds;
    private final AtomicReference<String> optOutUrl;

    public AttestationResponseHandler(Vertx vertx,
                                      String attestationEndpoint,
                                      String clientApiToken,
                                      String operatorType,
                                      ApplicationVersion appVersion,
                                      IAttestationProvider attestationProvider,
                                      Handler<Pair<AttestationResponseCode, String>> responseWatcher,
                                      Proxy proxy,
                                      UidInstanceIdProvider uidInstanceIdProvider) {
        this(vertx, attestationEndpoint, clientApiToken, operatorType, appVersion, attestationProvider, responseWatcher, proxy, new InstantClock(), null, null, 60000, uidInstanceIdProvider);
    }

    public AttestationResponseHandler(Vertx vertx,
                                      String attestationEndpoint,
                                      String clientApiToken,
                                      String operatorType,
                                      ApplicationVersion appVersion,
                                      IAttestationProvider attestationProvider,
                                      Handler<Pair<AttestationResponseCode, String>> responseWatcher,
                                      Proxy proxy,
                                      IClock clock,
                                      URLConnectionHttpClient httpClient,
                                      AttestationTokenDecryptor attestationTokenDecryptor,
                                      int attestCheckMilliseconds,
                                      UidInstanceIdProvider uidInstanceIdProvider) {
        this.vertx = vertx;
        this.attestationEndpoint = attestationEndpoint;
        this.encodedAttestationEndpoint = this.encodeStringUnicodeAttestationEndpoint(attestationEndpoint);
        this.clientApiToken = clientApiToken;
        this.operatorType = operatorType;
        this.appVersion = appVersion;
        this.attestationProvider = attestationProvider;
        this.attestationToken = new AtomicReference<>(null);
        this.optOutJwt = new AtomicReference<>(null);
        this.coreJwt = new AtomicReference<>(null);
        this.optOutUrl = new AtomicReference<>(null);
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
        this.uidInstanceIdProvider = uidInstanceIdProvider;
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
        } catch (AttestationResponseHandlerException | IOException e) {
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

    public void attest() throws IOException, AttestationResponseHandlerException {
        if (!attestationProvider.isReady()) {
            throw new AttestationResponseHandlerException("attestation provider is not ready");
        }

        try {
            KeyPair keyPair = generateKeyPair();
            byte[] publicKey = keyPair.getPublic().getEncoded();
            JsonObject requestJson = JsonObject.of(
                    "attestation_request", Base64.getEncoder().encodeToString(attestationProvider.getAttestationRequest(publicKey, this.encodedAttestationEndpoint)),
                    "public_key", Base64.getEncoder().encodeToString(publicKey),
                    "application_name", appVersion.getAppName(),
                    "application_version", appVersion.getAppVersion(),
                    "operator_type", this.operatorType
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
            headers.put(Audit.UID_INSTANCE_ID_HEADER, this.uidInstanceIdProvider.getInstanceId());

            HttpResponse<String> response = httpClient.post(attestationEndpoint, requestJson.toString(), headers);

            int statusCode = response.statusCode();
            String responseBody = response.body();

            AttestationResponseCode responseCode = this.getAttestationResponseCodeFromHttpStatus(statusCode);

            notifyResponseWatcher(responseCode, responseBody);

            if (responseCode != AttestationResponseCode.Success) {
                throw new AttestationResponseHandlerException(responseCode, "Non-success response from Core on attest");
            }

            JsonObject responseJson = (JsonObject) Json.decodeValue(responseBody);
            if (isFailed(responseJson)) {
                throw new AttestationResponseHandlerException(AttestationResponseCode.RetryableFailure, "response did not return a successful status");
            }

            JsonObject innerBody = responseJson.getJsonObject("body");
            if (innerBody == null) {
                throw new AttestationResponseHandlerException(AttestationResponseCode.RetryableFailure, "response did not contain a body object");
            }

            String atoken = getAttestationToken(innerBody);
            if (atoken == null) {
                throw new AttestationResponseHandlerException(AttestationResponseCode.RetryableFailure, "response json does not contain body.attestation_token");
            }
            String expiresAt = getAttestationTokenExpiresAt(innerBody);
            if (expiresAt == null) {
                throw new AttestationResponseHandlerException(AttestationResponseCode.RetryableFailure, "response json does not contain body.expiresAt");
            }

            atoken = new String(attestationTokenDecryptor.decrypt(Base64.getDecoder().decode(atoken), keyPair.getPrivate()), StandardCharsets.UTF_8);
            LOGGER.info("Attestation successful. Attestation token received.");
            setAttestationToken(atoken);
            setAttestationTokenExpiresAt(expiresAt);
            setOptoutJWTFromResponse(innerBody);
            setCoreJWTFromResponse(innerBody);
            setOptoutURLFromResponse(innerBody);

            scheduleAttestationExpirationCheck();
        } catch (AttestationResponseHandlerException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new AttestationResponseHandlerException(e);
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

    public String getOptOutUrl() {
        return this.optOutUrl.get();
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

    private void setOptoutURLFromResponse(JsonObject responseBody) {
        String url = responseBody.getString("optout_url");
        if (url == null) {
            LOGGER.info("OptOut URL not received");
        } else {
            LOGGER.info("OptOut URL received");
            LOGGER.debug("OptOut URL to use: {}", url);
            this.optOutUrl.set(url);
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

    private void notifyResponseWatcher(AttestationResponseCode responseCode, String responseBody) {
        if (responseCode != AttestationResponseCode.Success) {
            LOGGER.warn("Received a non-success response code on Attestation: ResponseCode: {}, Message: {}", responseCode, responseBody);
        }

        this.lock.lock();
        try {
            if (this.responseWatcher != null)
                this.responseWatcher.handle(Pair.of(responseCode, responseBody));
        } finally {
            lock.unlock();
        }
    }

    public boolean attested() {
        return this.attestationToken.get() != null && this.clock.now().isBefore(this.attestationTokenExpiresAt);
    }

    private byte[] encodeStringUnicodeAttestationEndpoint(String data) {
        // buffer.array() may include extra empty bytes at the end. This returns only the bytes that have data
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(data);
        return Arrays.copyOf(buffer.array(), buffer.limit());
    }

    private AttestationResponseCode getAttestationResponseCodeFromHttpStatus(int httpStatus) {
        if (httpStatus == 401 || httpStatus == 403) {
            return AttestationResponseCode.AttestationFailure;
        }

        if (httpStatus == 200) {
            return AttestationResponseCode.Success;
        }

        return AttestationResponseCode.RetryableFailure;
    }
}
