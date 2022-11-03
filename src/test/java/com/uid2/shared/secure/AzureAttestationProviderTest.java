package com.uid2.shared.secure;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class AzureAttestationProviderTest {
    private String attestationRequest = "test-attestation-request";
    private static final String publicKey = "test-public-key";
    private static final String expectedMrEnclave = "test-enclave";

    private Vertx vertx;
    private HttpServer server;
    private WebClient client;
    private AzureAttestationProvider provider;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 18000;
    private static final String URL = String.format("http://%s:%d", HOST, PORT);

    @Before
    public void setup(TestContext ctx) throws AttestationException {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        client = WebClient.create(vertx);
        provider = new AzureAttestationProvider(URL, client);
        provider.registerEnclave(expectedMrEnclave);
    }

    @After
    public void tearDown(TestContext ctx) {
        vertx.close(ctx.asyncAssertSuccess());
    }

    //region test helpers

    private void listen(TestContext ctx, Handler<Void> handler) {
        server.listen(PORT, HOST, event -> {
            ctx.assertTrue(event.succeeded());
            handler.handle(null);
        });
    }

    private void stopServer(Async async) {
        server.close(ar -> {async.complete();});
    }

    private void attest(Handler<AsyncResult<AttestationResult>> handler) {
        provider.attest(
                attestationRequest.getBytes(StandardCharsets.UTF_8),
                publicKey.getBytes(StandardCharsets.UTF_8),
                handler);
    }

    private void listenAndAttest(TestContext ctx, Handler<AsyncResult<AttestationResult>> handler) {
        listen(ctx, x -> { attest(handler); });
    }

    private String urlDecodeToString(String src) {
        return new String(Base64.getUrlDecoder().decode(src));
    }

    private void checkRequestBody(TestContext ctx, Buffer body) {
        JsonObject json = new JsonObject(body);
        ctx.assertEquals(attestationRequest, urlDecodeToString(json.getString("Quote")));
        ctx.assertEquals(publicKey, urlDecodeToString(json.getJsonObject("RuntimeData").getString("Data")));
    }

    private void onAttestationRequest(TestContext ctx, Async async, Handler<HttpServerRequest> handler) {
        server.requestHandler(request -> {
            request.bodyHandler(body -> {
                checkRequestBody(ctx, body);
                handler.handle(request);
                stopServer(async);
            });
        });
    }

    private static <T> void putJsonValue(JsonObject json, String key, T value) {
        if (value == null) {
            json.putNull(key);
        } else {
            json.put(key, value);
        }
    }

    private static String makeJwtString(String mrEnclave, Integer productId, Integer svn, Boolean isDebuggable, String ehd) {
        JsonObject jwt = new JsonObject();
        putJsonValue(jwt, "x-ms-sgx-mrenclave", mrEnclave);
        putJsonValue(jwt, "x-ms-sgx-product-id", productId);
        putJsonValue(jwt, "x-ms-sgx-svn", svn);
        putJsonValue(jwt, "x-ms-sgx-is-debuggable", isDebuggable);
        putJsonValue(jwt, "x-ms-sgx-ehd", ehd == null ? null : Base64.getUrlEncoder().encodeToString(ehd.getBytes(StandardCharsets.UTF_8)));
        return "header." + Base64.getUrlEncoder().encodeToString(jwt.toString().getBytes(StandardCharsets.UTF_8)) + ".signature";
    }

    private static class Jwt {
        String mrEnclave = AzureAttestationProviderTest.expectedMrEnclave;
        Integer productId = AzureAttestationProvider.REQUIRED_PRODUCT_ID;
        Integer svn = AzureAttestationProvider.REQUIRED_SECURITY_VERSION;
        Boolean isDebuggable = false;
        String ehd = AzureAttestationProviderTest.publicKey;

        static Jwt make() { return new Jwt(); }
        Jwt setMrEnclave(String mrEnclave) { this.mrEnclave = mrEnclave; return this; }
        Jwt setProductId(Integer productId) { this.productId = productId; return this; }
        Jwt setSecurityVersion(Integer svn) { this.svn = svn; return this; }
        Jwt setIsDebuggable(Boolean isDebuggable) { this.isDebuggable = isDebuggable; return this; }
        Jwt setEnclaveHeldData(String ehd) { this.ehd = ehd; return this; }

        @Override
        public String toString() { return makeJwtString(mrEnclave, productId, svn, isDebuggable, ehd); }
    }

    private void issueTokenOnAttestationRequest(TestContext ctx, Async async, Jwt jwt)
    {
        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(200).end("{\"token\": \"" + jwt.toString() + "\"}");
        });
    }

    //endregion test helpers

    @Test
    public void attestationErrorResponse(TestContext ctx) {
        Async async = ctx.async();

        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(400).end();
        });

        listenAndAttest(ctx, ar -> {
           ctx.assertTrue(ar.succeeded());
           AttestationResult result = ar.result();
           ctx.assertFalse(result.isSuccess());
           ctx.assertEquals(AttestationFailure.BAD_PAYLOAD, result.getFailure());
        });
    }

    @Test
    public void attestationNonJsonResponse(TestContext ctx) {
        Async async = ctx.async();

        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(200).end("bogus");
        });

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationNoTokenInJsonResponse(TestContext ctx) {
        Async async = ctx.async();

        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(200).end("{\"foo\": \"bar\"}");
        });

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationEmptyTokenInJsonResponse(TestContext ctx) {
        Async async = ctx.async();

        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(200).end("{\"token\": \"\"}");
        });

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationInvalidTokenFormatInJsonResponse(TestContext ctx) {
        Async async = ctx.async();

        onAttestationRequest(ctx, async, request -> {
            request.response().setStatusCode(200).end("{\"token\": \"abc.def.ghi.klm\"}");
        });

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationUnknownMrEnclave(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setMrEnclave("bogusEnclave"));

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertFalse(result.isSuccess());
            ctx.assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, result.getFailure());
        });
    }

    @Test
    public void attestationMissingMrEnclave(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setMrEnclave(null));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationEmptyMrEnclave(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setMrEnclave(""));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationUnknownProductId(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setProductId(AzureAttestationProvider.REQUIRED_PRODUCT_ID+1));

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertFalse(result.isSuccess());
            ctx.assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, result.getFailure());
        });
    }

    @Test
    public void attestationMissingProductId(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setProductId(null));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationUnknownSecurityVersion(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setSecurityVersion(AzureAttestationProvider.REQUIRED_SECURITY_VERSION + 1));

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertFalse(result.isSuccess());
            ctx.assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, result.getFailure());
        });
    }

    @Test
    public void attestationMissingSecurityVersion(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setSecurityVersion(null));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationIsDebuggableTrue(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setIsDebuggable(true));

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertFalse(result.isSuccess());
            ctx.assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, result.getFailure());
        });
    }

    @Test
    public void attestationMissingIsDebuggable(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setIsDebuggable(null));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationEnclaveHeldDataMismatchesPublicKey(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setEnclaveHeldData("bogusData"));

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertFalse(result.isSuccess());
            ctx.assertEquals(AttestationFailure.BAD_PAYLOAD, result.getFailure());
        });
    }

    @Test
    public void attestationMissingEnclaveHeldData(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setEnclaveHeldData(null));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationEmptyEnclaveHeldData(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make().setEnclaveHeldData(""));

        listenAndAttest(ctx, ar -> {
            ctx.assertFalse(ar.succeeded());
            ctx.assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void attestationSuccess(TestContext ctx) {
        Async async = ctx.async();

        issueTokenOnAttestationRequest(ctx, async, Jwt.make());

        listenAndAttest(ctx, ar -> {
            ctx.assertTrue(ar.succeeded());
            AttestationResult result = ar.result();
            ctx.assertTrue(result.isSuccess());
            ctx.assertEquals(publicKey, new String(result.getPublicKey()));
        });
    }
}
