package com.uid2.shared.secure;

import com.uid2.shared.secure.gcpoidc.IPolicyValidator;
import com.uid2.shared.secure.gcpoidc.ITokenSignatureValidator;
import com.uid2.shared.secure.gcpoidc.TokenPayload;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GcpOidcAttestationProviderTest {
    private static final String ATTESTATION_REQUEST = "test-attestation-request";
    private static final String PUBLIC_KEY = "test-public-key";

    private static final String ENCLAVE_ID_1 = "test-enclave_1";

    private static final String ENCLAVE_ID_2 = "test-enclave_2";

    private static final TokenPayload VALID_TOKEN_PAYLOAD = TokenPayload.builder().build();

    @Mock private ITokenSignatureValidator alwaysPassTokenValidator;
    @Mock private ITokenSignatureValidator alwaysFailTokenValidator;

    @Mock private IPolicyValidator alwaysPassPolicyValidator1;

    @Mock private IPolicyValidator alwaysPassPolicyValidator2;

    @Mock private IPolicyValidator alwaysFailPolicyValidator;


    @Before
    public void setup() throws AttestationException {
        when(alwaysPassTokenValidator.validate(any())).thenReturn(VALID_TOKEN_PAYLOAD);
        when(alwaysFailTokenValidator.validate(any())).thenThrow(new AttestationException("token signature validation failed"));
        when(alwaysPassPolicyValidator1.validate(any())).thenReturn(ENCLAVE_ID_1);
        when(alwaysPassPolicyValidator2.validate(any())).thenReturn(ENCLAVE_ID_2);
        when(alwaysFailPolicyValidator.validate(any())).thenThrow(new AttestationException("policy validation failed"));
    }

    @Test
    public void testHappyPath() throws AttestationException {
        var provider = new GcpOidcAttestationProvider(alwaysPassTokenValidator, Arrays.asList(alwaysPassPolicyValidator1));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar ->{
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    @Test
    public void testSignatureCheckFailed() throws AttestationException {
        var provider = new GcpOidcAttestationProvider(alwaysFailTokenValidator, Arrays.asList(alwaysPassPolicyValidator1));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar ->{
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testPolicyCheckFailed() throws AttestationException {
        var provider = new GcpOidcAttestationProvider(alwaysPassTokenValidator, Arrays.asList(alwaysFailPolicyValidator));
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar ->{
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testNoPolicyConfigured() throws AttestationException {
        var provider = new GcpOidcAttestationProvider(alwaysPassTokenValidator, Arrays.asList());
        provider.registerEnclave(ENCLAVE_ID_1);
        attest(provider, ar ->{
            assertFalse(ar.succeeded());
            assertTrue(ar.cause() instanceof AttestationException);
        });
    }

    @Test
    public void testMultiplePolicyValidators() throws AttestationException {
        var provider = new GcpOidcAttestationProvider(alwaysPassTokenValidator, Arrays.asList(alwaysPassPolicyValidator1, alwaysFailPolicyValidator, alwaysPassPolicyValidator2));
        provider.registerEnclave(ENCLAVE_ID_2);
        attest(provider, ar ->{
            assertTrue(ar.succeeded());
            assertTrue(ar.result().isSuccess());
        });
    }

    private static void attest(IAttestationProvider provider, Handler<AsyncResult<AttestationResult>> handler) {
        provider.attest(
                ATTESTATION_REQUEST.getBytes(StandardCharsets.UTF_8),
                PUBLIC_KEY.getBytes(StandardCharsets.UTF_8),
                handler);
    }
}
