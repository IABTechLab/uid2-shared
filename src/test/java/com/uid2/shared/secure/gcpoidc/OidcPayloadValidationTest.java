package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.TestClock;
import org.junit.jupiter.api.Test;

import static com.uid2.shared.secure.TestUtils.loadFromJson;
import static com.uid2.shared.secure.gcpoidc.OidcTokenUtils.validateAndParseToken;

public class OidcPayloadValidationTest {
    // E2E to help prevent regression.
    @Test
    public void testE2EPolicyCheck() throws Exception {
        // expire at 1688132564
        var payloadPath = "/com.uid2.shared/test/secure/gcpoidc/jwt_payload_policy_valid.json";
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1688132563000L);

        var tokenPayload = validateAndParseToken(payload, clock);
        var policyValidator = new PolicyValidator();
        var enclaveId = policyValidator.validate(tokenPayload);
    }
}
