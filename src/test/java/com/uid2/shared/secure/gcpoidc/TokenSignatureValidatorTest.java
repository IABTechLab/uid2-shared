package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Clock;
import com.google.api.client.util.SecurityUtils;
import com.google.api.client.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uid2.shared.Const;
import com.uid2.shared.secure.AttestationException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.junit.Test;

import java.io.IOException;
import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

public class TokenSignatureValidatorTest {
    @Test
    public void testFullPayload() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoaid/jwt_payload_full.json";
        var payload = loadTestJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563000L);

        var tokenPayload = testTokenValidator(payload, clock);

        assertEquals("disabled-since-boot", tokenPayload.getDbgStat());
        assertEquals("CONFIDENTIAL_SPACE", tokenPayload.getSwName());
        assertEquals("230600", tokenPayload.getSwVersion());

        assertEquals("us-docker.pkg.dev/someone-primus-bank/primus-workloads/initial-workload-container:latest", tokenPayload.getWorkloadImageReference());
        assertEquals("sha256:fedbd6eaf93394d5eb98d7e52d4cce17e0ea23f7fac1a6bd68e097ca85a4fffb", tokenPayload.getWorkloadImageDigest());
        assertEquals("Never", tokenPayload.getRestartPolicy());

        assertEquals(3, tokenPayload.getCmdOverrides().size());
        assertEquals("Seattle", tokenPayload.getCmdOverrides().get(1));
        assertEquals(2, tokenPayload.getEnvOverrides().size());
        assertEquals("value1", tokenPayload.getEnvOverrides().get("env1"));
    }

    @Test
    public void testPayloadWithNullValue() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoaid/jwt_payload_null.json";
        var payload = loadTestJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563000L);

        var tokenPayload = testTokenValidator(payload, clock);

        assertTrue(CollectionUtils.isEmpty(tokenPayload.getCsSupportedAttributes()));
        assertTrue(CollectionUtils.isEmpty(tokenPayload.getCmdOverrides()));
        assertTrue(MapUtils.isEmpty(tokenPayload.getEnvOverrides()));
    }

    @Test
    public void testTokenExpired() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoaid/jwt_payload_null.json";
        var payload = loadTestJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132565_000L);
        assertThrows(AttestationException.class, ()->testTokenValidator(payload, clock));
    }

    @Test
    public void testAudienceNotMatch() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoaid/jwt_payload_null.json";
        var payload = loadTestJson(payloadPath);
        payload.addProperty("aud","dummy");
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563_000L);
        assertThrows(AttestationException.class, ()->testTokenValidator(payload, clock));
    }

    @Test
    public void testIssuerNotMatch() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoaid/jwt_payload_null.json";
        var payload = loadTestJson(payloadPath);
        payload.addProperty("iss","dummy");
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563_000L);
        assertThrows(AttestationException.class, ()->testTokenValidator(payload, clock));
    }

    private TokenPayload testTokenValidator(JsonObject payload, Clock clock) throws Exception{
        var gen = KeyPairGenerator.getInstance(Const.Name.AsymetricEncryptionKeyClass);
        gen.initialize(2048, new SecureRandom());
        var keyPair = gen.generateKeyPair();
        var privateKey = keyPair.getPrivate();
        var publicKey = keyPair.getPublic();

        // generate token
        var token = generateJwt(payload, privateKey);

        // init TokenSignatureValidator
        var tokenVerifier = new TokenSignatureValidator(publicKey, clock);

        // validate token
        return tokenVerifier.validate(token);
    }

    private String generateJwt(JsonObject payload, PrivateKey privateKey) throws Exception {
        var jsonFactory = new GsonFactory();
        var header = new JsonWebSignature.Header();
        header.setAlgorithm("RS256");
        header.setType("JWT");
        header.setKeyId("dummy");
        String content = Base64.encodeBase64URLSafeString(jsonFactory.toByteArray(header)) + "." + Base64.encodeBase64URLSafeString(payload.toString().getBytes());
        byte[] contentBytes = StringUtils.getBytesUtf8(content);
        byte[] signature = SecurityUtils.sign(SecurityUtils.getSha256WithRsaSignatureAlgorithm(), privateKey, contentBytes);
        return content + "." + Base64.encodeBase64URLSafeString(signature);
    }

    private JsonObject loadTestJson(String fileName) throws IOException {
        var jsonStr = new String(getClass().getResourceAsStream(fileName).readAllBytes());
        var payload = new Gson().fromJson(jsonStr, JsonObject.class);
        return payload;
    }

    public class TestClock implements Clock{
        long currentTimeMs;

        @Override
        public long currentTimeMillis() {
            return currentTimeMs;
        }

        public void setCurrentTimeMs(long currentTimeMs){
            this.currentTimeMs = currentTimeMs;
        }
    }
}
