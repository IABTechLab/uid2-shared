package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.TestClock;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.uid2.shared.secure.TestUtils.loadFromJson;
import static com.uid2.shared.secure.azurecc.MaaTokenUtils.validateAndParseToken;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaaTokenSignatureValidatorTest {
    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testPayload(String payloadPath, String protocol) throws Exception {
        // expire at 1695313895
        var payload = loadFromJson(payloadPath);
        var clock = new TestClock();
        clock.setCurrentTimeMs(1695313893000L);

        var expectedCcePolicy = "fef932e0103f6132437e8a1223f32efc4bea63342f893b5124645224ef29ba73";
        var expectedLocation = "East US";
        var expectedPublicKey = "abc";

        var tokenPayload = validateAndParseToken(payload, clock, protocol);
        assertEquals(true, tokenPayload.isSevSnpVM());
        assertEquals(true, tokenPayload.isUtilityVMCompliant());
        assertEquals(false, tokenPayload.isVmDebuggable());
        assertEquals(expectedCcePolicy, tokenPayload.getCcePolicyDigest());
        assertEquals(expectedLocation, tokenPayload.getRuntimeData().getLocation());
        assertEquals(expectedPublicKey, tokenPayload.getRuntimeData().getPublicKey());
    }

    @Disabled
    // replace below Placeholder with real MAA token to run E2E verification.
    public void testE2E() throws AttestationException {
        var maaToken = "<Placeholder>";
        var maaServerUrl = "https://sharedeus.eus.attest.azure.net";
        var validator = new MaaTokenSignatureValidator(maaServerUrl);
        var token = validator.validate(maaToken, MaaTokenPayload.AZURE_CC_ACI_PROTOCOL);
    }

    static Stream<Arguments> argumentProvider() {
        return Stream.of(
                Arguments.of("/com.uid2.shared/test/secure/azurecc/jwt_payload_aci.json", MaaTokenPayload.AZURE_CC_ACI_PROTOCOL),
                Arguments.of("/com.uid2.shared/test/secure/azurecc/jwt_payload_aks.json", MaaTokenPayload.AZURE_CC_AKS_PROTOCOL)
        );
    }
}
