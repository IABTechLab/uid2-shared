package com.uid2.shared.secure;

import com.uid2.shared.secure.azurecc.IMaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.IPolicyValidator;
import com.uid2.shared.secure.azurecc.MaaTokenPayload;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureCCAttestationProviderTest {
    private static final String ATTESTATION_REQUEST = "test-attestation-request";

    private static final String PUBLIC_KEY = "test-public-key";

    private static final String ENCLAVE_ID = "test-enclave";

    private static final MaaTokenPayload VALID_TOKEN_PAYLOAD = MaaTokenPayload.builder().build();
    @Mock
    private IMaaTokenSignatureValidator alwaysPassTokenValidator;

    @Mock
    private IMaaTokenSignatureValidator alwaysFailTokenValidator;

    @Mock
    private IPolicyValidator alwaysPassPolicyValidator;

    @Mock
    private IPolicyValidator alwaysFailPolicyValidator;

    @BeforeEach
    public void setup() throws AttestationException {
        when(alwaysPassTokenValidator.validate(any())).thenReturn(VALID_TOKEN_PAYLOAD);
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationException("token signature validation failed"));
        when(alwaysPassPolicyValidator.validate(any(), any())).thenReturn(ENCLAVE_ID);
        when(alwaysFailPolicyValidator.validate(any(), any())).thenThrow(new AttestationException("policy validation failed"));
    }

    @Test
    public void testHappyPath() throws AttestationException {
        var provider = new AzureCCAttestationProvider(alwaysPassTokenValidator, alwaysPassPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    @Test
    public void testSignatureCheckFailed() throws AttestationException {
        var provider = new AzureCCAttestationProvider(alwaysFailTokenValidator, alwaysPassPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testPolicyCheckFailed() throws AttestationException {
        var provider = new AzureCCAttestationProvider(alwaysFailTokenValidator, alwaysFailPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testEnclaveNotRegistered() throws AttestationException {
        var provider = new AzureCCAttestationProvider(alwaysFailTokenValidator, alwaysPassPolicyValidator);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    private static void attest(IAttestationProvider provider, Handler<AsyncResult<AttestationResult>> handler) {
        provider.attest(
                ATTESTATION_REQUEST.getBytes(StandardCharsets.UTF_8),
                PUBLIC_KEY.getBytes(StandardCharsets.UTF_8),
                handler);
    }
}
