package com.uid2.shared.secure;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import com.uid2.shared.Utils;

public class AzureAttestationProvider implements IAttestationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAttestationProvider.class);
    private Set<String> allowedEnclaveIds;
    private String url;
    private WebClient webClient;

    public static final int REQUIRED_PRODUCT_ID = 770;
    public static final int REQUIRED_SECURITY_VERSION = 2;

    public AzureAttestationProvider(String maaServerBaseUrl, WebClient webClient) {
        this.allowedEnclaveIds = new HashSet<>();
        this.url = String.format("%s/attest/SgxEnclave?api-version=2020-10-01", maaServerBaseUrl);
        this.webClient = webClient;
        LOGGER.info("Using MAA server URL: " + this.url);
    }

    @Override
    public void attest(byte[] attestationRequest, byte[] publicKey, Handler<AsyncResult<AttestationResult>> handler) {
        JsonObject runtimeDataJson = new JsonObject();
        runtimeDataJson.put("Data", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey));
        runtimeDataJson.put("DataType", "Binary");

        JsonObject requestJson = new JsonObject();
        requestJson.put("Quote", Base64.getUrlEncoder().withoutPadding().encodeToString(attestationRequest));
        requestJson.put("RuntimeData", runtimeDataJson);
        requestJson.putNull("InittimeData");
        requestJson.putNull("DraftPolicyForAttestation");

        this.webClient.postAbs(this.url)
                .sendJsonObject(requestJson, ar -> this.handleAttestationResponse(ar, handler, publicKey));
    }

    private void handleAttestationResponse(AsyncResult<HttpResponse<Buffer>> ar,
                                           Handler<AsyncResult<AttestationResult>> handler,
                                           byte[] publicKey)
    {
        if (!ar.succeeded()) {
            handler.handle(Future.failedFuture(new AttestationException(ar.cause())));
            return;
        }

        try {
            final HttpResponse<Buffer> response = ar.result();
            if (response.statusCode() != 200) {
                LOGGER.warn("Failed attestation response from MAA server, status " + response.statusCode() + ": " + response.bodyAsString());
                handler.handle(Future.succeededFuture(new AttestationResult(AttestationFailure.BAD_PAYLOAD)));
                return;
            }

            final JsonObject responseJson = response.bodyAsJsonObject();
            final AttestationResult result = validateResponse(responseJson, publicKey);
            handler.handle(Future.succeededFuture(result));
        } catch (AttestationException e) {
            handler.handle(Future.failedFuture(e));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(new AttestationException(e)));
        }
    }

    private AttestationResult validateResponse(JsonObject responseJson, byte[] publicKey) throws AttestationException {
        String serviceJwt = responseJson.getString("token");
        if (serviceJwt == null || serviceJwt.isEmpty()) {
            throw new AttestationException("no jwt present in the response");
        }

        // expecting header, body, signature -- separated by .
        String[] serviceJwtParts = serviceJwt.split("\\.");
        if (serviceJwtParts.length != 3) {
            throw new AttestationException("invalid jwt structure");
        }

        JsonObject jwtBody = new JsonObject(new String(Base64.getUrlDecoder().decode(serviceJwtParts[1]), StandardCharsets.UTF_8));

        // verify mrEnclave
        String mrEnclave = jwtBody.getString("x-ms-sgx-mrenclave");
        if (mrEnclave == null || mrEnclave.isEmpty()) {
            throw new AttestationException("no mrenclave present in the response");
        } else if (!this.allowedEnclaveIds.contains(mrEnclave.toLowerCase())) {
            LOGGER.warn("Got unsupported MRENCLAVE='" + mrEnclave + "'");
            return new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE);
        }

        // verify product id and security version
        Integer productId = jwtBody.getInteger("x-ms-sgx-product-id");
        if (productId == null) {
            throw new AttestationException("no product id present in the response");
        }
        Integer svn = jwtBody.getInteger("x-ms-sgx-svn");
        if (svn == null) {
            throw new AttestationException("no security version present in the response");
        }
        if (productId != REQUIRED_PRODUCT_ID || svn != REQUIRED_SECURITY_VERSION) {
            LOGGER.warn("Got unsupported ProductId=" + productId + " and SecurityVersion=" + svn + " combination");
            return new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE);
        }

        // must not be debuggable
        Boolean isDebuggable = jwtBody.getBoolean("x-ms-sgx-is-debuggable");
        if (isDebuggable == null) {
            throw new AttestationException("no is debuggable flag present in the response");
        } else if (isDebuggable == true) {
            LOGGER.warn("Got a debuggable enclave which is not allowed");
            return new AttestationResult(AttestationFailure.FORBIDDEN_ENCLAVE);
        }

        // verify that publicKey matches
        String ehd = jwtBody.getString("x-ms-sgx-ehd");
        if (ehd == null || ehd.isEmpty()) {
            throw new AttestationException("no ehd present in the response");
        } else if (!Arrays.equals(Base64.getUrlDecoder().decode(ehd), publicKey)) {
            LOGGER.warn("Got mismatch between reported EHD and public key, ehd=" + ehd + " publicKey=" + Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey));
            return new AttestationResult(AttestationFailure.BAD_PAYLOAD);
        }

        return new AttestationResult(publicKey);
    }

    @Override
    public void registerEnclave(String id) throws AttestationException {
        try {
            this.allowedEnclaveIds.add(id.toLowerCase());
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public void unregisterEnclave(String id) throws AttestationException {
        try {
            this.allowedEnclaveIds.remove(id.toLowerCase());
        } catch (Exception e) {
            throw new AttestationException(e);
        }
    }

    @Override
    public Collection<String> getEnclaveAllowlist() {
        return this.allowedEnclaveIds;
    }
}
