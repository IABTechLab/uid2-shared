package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.TestClock;
import org.junit.jupiter.api.Test;

import static com.uid2.shared.secure.TestUtils.loadFromJson;
import static com.uid2.shared.secure.azurecc.MaaTokenUtils.validateAndParseToken;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaaTokenSignatureValidatorTest {
    @Test
    public void testPayload() throws Exception {
        // expire at 1695313895
        var payloadPath = "/com.uid2.shared/test/secure/azurecc/jwt_payload.json";
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1695313893000L);

        var expectedCcePolicy = "fef932e0103f6132437e8a1223f32efc4bea63342f893b5124645224ef29ba73";
        var expectedLocation = "East US";
        var expectedPublicKey = "abc";

        var tokenPayload = validateAndParseToken(payload, clock);
        assertEquals(true, tokenPayload.isSevSnpVM());
        assertEquals(true, tokenPayload.isUtilityVMCompliant());
        assertEquals(false, tokenPayload.isVmDebuggable());
        assertEquals(expectedCcePolicy, tokenPayload.getCcePolicy());
        assertEquals(expectedLocation, tokenPayload.getRuntimeData().getLocation());
        assertEquals(expectedPublicKey, tokenPayload.getRuntimeData().getPublicKey());
    }
}
