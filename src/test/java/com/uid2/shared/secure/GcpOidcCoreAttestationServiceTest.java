package com.uid2.shared.secure;

import com.uid2.shared.secure.gcpoidc.IPolicyValidator;
import com.uid2.shared.secure.gcpoidc.ITokenSignatureValidator;
import com.uid2.shared.secure.gcpoidc.TokenPayload;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GcpOidcCoreAttestationServiceTest {
    private static final String ATTESTATION_REQUEST = "test-attestation-request";
    private static final String PUBLIC_KEY = "test-public-key";

    private static final String ENCLAVE_ID_1 = "test-enclave_1";

    private static final String ENCLAVE_ID_2 = "test-enclave_2";

    private static final TokenPayload VALID_TOKEN_PAYLOAD = TokenPayload.builder().build();

    @Mock
    private ITokenSignatureValidator alwaysPassTokenValidator;
    @Mock
    private ITokenSignatureValidator alwaysFailTokenValidator;

    @Mock
    private IPolicyValidator alwaysPassPolicyValidator1;

    @Mock
    private IPolicyValidator alwaysPassPolicyValidator2;

    @Mock
    private IPolicyValidator alwaysFailPolicyValidator;


    @BeforeEach
    public void setup() throws AttestationException {
        when(alwaysPassTokenValidator.validate(any())).thenReturn(VALID_TOKEN_PAYLOAD);
        when(alwaysPassPolicyValidator1.validate(any())).thenReturn(ENCLAVE_ID_1);
        when(alwaysPassPolicyValidator2.validate(any())).thenReturn(ENCLAVE_ID_2);
    }

    @Test
    public void testHappyPath() throws AttestationException {
        var provider = new GcpOidcCoreAttestationService(alwaysPassTokenValidator, List.of(alwaysPassPolicyValidator1));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    @Test
    public void testSignatureCheckFailed_ClientError() throws AttestationException {
        var errorStr = "signature validation failed";
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationClientException(errorStr, AttestationFailure.BAD_PAYLOAD));
        var provider = new GcpOidcCoreAttestationService(alwaysFailTokenValidator, List.of(alwaysPassPolicyValidator1));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @Test
    public void testSignatureCheckFailed_ServerError() throws AttestationException {
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new GcpOidcCoreAttestationService(alwaysFailTokenValidator, List.of(alwaysPassPolicyValidator1));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertInstanceOf(AttestationException.class, ar.cause());
        });
    }

    @Test
    public void testPolicyCheckFailed_ClientError() throws AttestationException {
        var errorStr = "policy validation failed";
        when(alwaysFailPolicyValidator.validate(any())).thenThrow(new AttestationClientException(errorStr, AttestationFailure.BAD_PAYLOAD));
        var provider = new GcpOidcCoreAttestationService(alwaysPassTokenValidator, List.of(alwaysFailPolicyValidator));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @Test
    public void testPolicyCheckFailed_ServerError() throws AttestationException {
        when(alwaysFailPolicyValidator.validate(any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new GcpOidcCoreAttestationService(alwaysPassTokenValidator, List.of(alwaysFailPolicyValidator));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertInstanceOf(AttestationException.class, ar.cause());
        });
    }

    @Test
    public void testNoPolicyConfigured() throws AttestationException {
        var provider = new GcpOidcCoreAttestationService(alwaysPassTokenValidator, List.of());
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(AttestationFailure.FORBIDDEN_ENCLAVE, ar.result().getFailure());
        });
    }

    @Test
    public void testMultiplePolicyValidators() throws AttestationException {
        var provider = new GcpOidcCoreAttestationService(alwaysPassTokenValidator, List.of(alwaysPassPolicyValidator1, alwaysFailPolicyValidator, alwaysPassPolicyValidator2));
        provider.registerEnclave(ENCLAVE_ID_2);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    private static void attest(ICoreAttestationService provider, Handler<AsyncResult<AttestationResult>> handler) {
        provider.attest(
                ATTESTATION_REQUEST.getBytes(StandardCharsets.UTF_8),
                PUBLIC_KEY.getBytes(StandardCharsets.UTF_8),
                handler);
    }
}
