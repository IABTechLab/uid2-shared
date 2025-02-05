package com.uid2.shared.secure;

import com.uid2.shared.secure.azurecc.IMaaTokenSignatureValidator;
import com.uid2.shared.secure.azurecc.IPolicyValidator;
import com.uid2.shared.secure.azurecc.MaaTokenPayload;
import com.uid2.shared.secure.azurecc.RuntimeData;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureCCCoreAttestationServiceTest {
    private static byte[] encodeStringUnicodeAttestationEndpoint(String data) {
        // buffer.array() may include extra empty bytes at the end. This returns only the bytes that have data
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(data);
        return Arrays.copyOf(buffer.array(), buffer.limit());
    }
    private static final String ATTESTATION_REQUEST = "test-attestation-request";

    private static final String PUBLIC_KEY = "test-public-key";

    private static final String ENCLAVE_ID = "test-enclave";
    private static final String ATTESTATION_URL = "https://example.com";
    private static final String ENCODED_ATTESTATION_URL = Base64.getEncoder().encodeToString(encodeStringUnicodeAttestationEndpoint(ATTESTATION_URL));
    private static final RuntimeData RUNTIME_DATA = RuntimeData.builder().attestationUrl(ENCODED_ATTESTATION_URL).build();

    private static final MaaTokenPayload VALID_TOKEN_PAYLOAD = MaaTokenPayload.builder().runtimeData(RUNTIME_DATA).build();
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
        when(alwaysPassTokenValidator.validate(any(), any())).thenReturn(VALID_TOKEN_PAYLOAD);
        when(alwaysPassPolicyValidator.validate(any(), any())).thenReturn(ENCLAVE_ID);
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testHappyPath(String azureProtocol) throws AttestationException {
        var provider = new AzureCCCoreAttestationService(alwaysPassTokenValidator, alwaysPassPolicyValidator, azureProtocol);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testSignatureCheckFailed_ClientError(String azureProtocol) throws AttestationException {
        var errorStr = "token signature validation failed";
        when(alwaysFailTokenValidator.validate(any(), any())).thenThrow(new AttestationClientException(errorStr, AttestationFailure.BAD_PAYLOAD));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator, azureProtocol);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testSignatureCheckFailed_ServerError(String azureProtocol) throws AttestationException {
        when(alwaysFailTokenValidator.validate(any(), any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator, azureProtocol);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testPolicyCheckSuccess_ClientError(String azureProtocol) throws AttestationException {
        var errorStr = "policy validation failed";
        when(alwaysFailPolicyValidator.validate(any(), any())).thenThrow(new AttestationClientException(errorStr, AttestationFailure.BAD_PAYLOAD));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysFailPolicyValidator, azureProtocol);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertTrue(ar.succeeded());
            assertFalse(ar.result().isSuccess());
            assertEquals(errorStr, ar.result().getReason());
        });
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testPolicyCheckFailed_ServerError(String azureProtocol) throws AttestationException {
        when(alwaysFailPolicyValidator.validate(any(), any())).thenThrow(new AttestationException("unknown server error"));
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysFailPolicyValidator, azureProtocol);
        provider.registerEnclave(ENCLAVE_ID);
        attest(provider, ar -> {
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void testEnclaveNotRegistered(String azureProtocol) throws AttestationException {
        var provider = new AzureCCCoreAttestationService(alwaysFailTokenValidator, alwaysPassPolicyValidator, azureProtocol);
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

    static Stream<Arguments> argumentProvider() {
        return Stream.of(
                Arguments.of("azure-cc"),
                Arguments.of("azure-cc-aks")
        );
    }
}
