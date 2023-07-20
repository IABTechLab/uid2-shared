package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.junit.Test;

import static com.uid2.shared.secure.gcpoidc.TestUtils.loadFromJson;
import static com.uid2.shared.secure.gcpoidc.TestUtils.validateAndParseToken;
import static org.junit.jupiter.api.Assertions.*;

public class TokenSignatureValidatorTest {
    @Test
    public void testFullPayload() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_full.json";
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563000L);

        var tokenPayload = validateAndParseToken(payload, clock);

        assertEquals("disabled-since-boot", tokenPayload.getDbgStat());
        assertEquals("CONFIDENTIAL_SPACE", tokenPayload.getSwName());
        assertEquals("230600", tokenPayload.getSwVersion());
        assertEquals(3, tokenPayload.getCsSupportedAttributes().size());
        assertEquals("LATEST", tokenPayload.getCsSupportedAttributes().get(0));

        assertEquals("us-docker.pkg.dev/someone-primus-bank/primus-workloads/initial-workload-container:latest", tokenPayload.getWorkloadImageReference());
        assertEquals("sha256:fedbd6eaf93394d5eb98d7e52d4cce17e0ea23f7fac1a6bd68e097ca85a4fffb", tokenPayload.getWorkloadImageDigest());
        assertEquals("Never", tokenPayload.getRestartPolicy());

        assertEquals(3, tokenPayload.getCmdOverrides().size());
        assertEquals("Seattle", tokenPayload.getCmdOverrides().get(1));
        assertEquals(2, tokenPayload.getEnvOverrides().size());
        assertEquals("value1", tokenPayload.getEnvOverrides().get("env1"));

        assertEquals(true, tokenPayload.isStableVersion());
        assertEquals(false, tokenPayload.isDebugMode());
    }

    @Test
    public void testPayloadWithNullValue() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_null.json";
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563000L);

        var tokenPayload = validateAndParseToken(payload, clock);

        assertTrue(CollectionUtils.isEmpty(tokenPayload.getCsSupportedAttributes()));
        assertTrue(CollectionUtils.isEmpty(tokenPayload.getCmdOverrides()));
        assertTrue(MapUtils.isEmpty(tokenPayload.getEnvOverrides()));
    }

    @Test
    public void testTokenExpired() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_null.json";
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132565_000L);
        assertThrows(AttestationException.class, ()->validateAndParseToken(payload, clock));
    }

    @Test
    public void testAudienceNotMatch() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_null.json";
        var payload = loadFromJson(payloadPath);
        payload.addProperty("aud","dummy");
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563_000L);
        assertThrows(AttestationException.class, ()->validateAndParseToken(payload, clock));
    }

    @Test
    public void testIssuerNotMatch() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_null.json";
        var payload = loadFromJson(payloadPath);
        payload.addProperty("iss","dummy");
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563_000L);
        assertThrows(AttestationException.class, ()->validateAndParseToken(payload, clock));
    }
}
