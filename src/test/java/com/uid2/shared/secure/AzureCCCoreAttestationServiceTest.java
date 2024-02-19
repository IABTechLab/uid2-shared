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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureCCCoreAttestationServiceTest {
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
        when(alwaysPassPolicyValidator.validate(any(), any())).thenReturn(ENCLAVE_ID);
    }

    @Test
    public void testHappyPath() throws AttestationException {
        var provider = new AzureCCCoreAttestationService(alwaysPassTokenValidator, alwaysPassPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    @Test
    public void testSignatureCheckFailed_ClientError() throws AttestationException {
        var errorStr = "token signature validation failed";
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationClientException(errorStr));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @Test
    public void testSignatureCheckFailed_ServerError() throws AttestationException {
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testPolicyCheckFailed_ClientError() throws AttestationException {
        var errorStr = "policy validation failed";
        when(alwaysFailPolicyValidator.validate(any(), any())).thenThrow(new AttestationClientException(errorStr));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysFailPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @Test
    public void testPolicyCheckFailed_ServerError() throws AttestationException {
        when(alwaysFailPolicyValidator.validate(any(), any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysFailPolicyValidator);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testEnclaveNotRegistered() throws AttestationException {
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, ar.result().getFailure());
        });
    }

    private static void attest(ICoreAttestationService provider, Handler<AsyncResult<AttestationResult>> handler) {
        provider.attest(
                ATTESTATION_REQUEST.getBytes(StandardCharsets.UTF_8),
                PUBLIC_KEY.getBytes(StandardCharsets.UTF_8),
                handler);
    }
}