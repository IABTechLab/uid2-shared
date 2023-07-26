package com.uid2.shared.attest;

import com.amazonaws.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JwtServiceTest {
    private final static String VALID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTY5MDM0ODk5OSwic3ViIjoiVGVzdCIsImF1ZCI6Imh0dHBzOi8vb3B0b3V0LWludGVnLnVpZGFwaS5jb20iLCJvcGVyYXRvclZlcnNpb24iOiJWZXJzaW9uIDEuMiIsImVuY2xhdmVUeXBlIjoidGVzdCBlbmNsYXZlIHR5cGUiLCJyb2xlcyI6Ik9QRVJBVE9SLE9QVE9VVCIsImlzcyI6Imh0dHBzOi8vY29yZS1pbnRlZy51aWRhcGkuY29tIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoidGVzdCBlbmNsYXZlIGlkIn0.N9xYROMx2hnMIhtyyBLF-J13uWXpIU6jj_Tgufww6O8JBhrHHFliOF2xsPUcZ1sK6lGsmbHACwlPTRz8zhpKWKM0CMNjfiWHwBGykK32hDC321QEta0aX6utBAWIb1crb2JwZhPH1K0_4X-mxdiuxibgW3YNpQxm2kZDnQaR40py5JykVkPxzwhgzUCceDN5kL1kNEjnO-X8fBDnTh_HIcRISfe1bluMbV4kzTv1EunlhG8rLpCvCOjK3ZLgipQa4vpKvaD7VH4wtvO13MbKjWNPLrFzhMwwonccPmM7ZHrHusq0SRFtspNZNwcV5AIKmHR2_Y1RdL-JnHmAC4cSYA";
    private final static String PUBLIC_KEY_STRING = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1T9DPF3nn6DcDviAXzEE\n" +
            "YQl5kHF0324dXeVpYuVINfq34hBrv6OpvRAb62KrOnQvalGjumuwQed9lKLwKUYl\n" +
            "tZorHlped8oA8C5smKoOqn1scBLCFBw1XH3hOVOsC8g0NXYvsg+WpM5mQD65hAaK\n" +
            "ew3YoLftO3YVNvDP/RJ9AdbE/2thZMUMIXa1RB34dCFrMe8jViYVhQf9WRc4khsa\n" +
            "lo0MaKz+lBmx+xQkqJHn9t+DiFY1HZUhM6sb3n2E+ttrtMxMh2WXMswyqxUBIrEN\n" +
            "P434UchtgCQvTdtAbMFu1B25HHFtPkIggulXt4Fli0WEn7PFUr9qbmgv7RYsQBED\n" +
            "4QIDAQAB\n" +
            "-----END PUBLIC KEY-----";

    private final static String COMPACT_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1T9DPF3nn6DcDviAXzEEYQl5kHF0324dXeVpYuVINfq34hBrv6OpvRAb62KrOnQvalGjumuwQed9lKLwKUYltZorHlped8oA8C5smKoOqn1scBLCFBw1XH3hOVOsC8g0NXYvsg+WpM5mQD65hAaKew3YoLftO3YVNvDP/RJ9AdbE/2thZMUMIXa1RB34dCFrMe8jViYVhQf9WRc4khsalo0MaKz+lBmx+xQkqJHn9t+DiFY1HZUhM6sb3n2E+ttrtMxMh2WXMswyqxUBIrENP434UchtgCQvTdtAbMFu1B25HHFtPkIggulXt4Fli0WEn7PFUr9qbmgv7RYsQBED4QIDAQAB";

    private final static String AUDIENCE = "https://optout-integ.uidapi.com";
    private final static String ISSUER = "https://core-integ.uidapi.com";

    private KmsClient mockClient;
    private ArgumentCaptor<GetPublicKeyRequest> capturedGetPublicKeyRequest;

    @Test
    void validateTokenSucceeds() throws JwtService.ValidationException {
        JwtService service = new JwtService(AUDIENCE, ISSUER).withPublicKey(PUBLIC_KEY_STRING);
        var validationResponse = service.validateJwt(VALID_TOKEN);

        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
        assertNull(validationResponse.getValidationException());
    }

    @Test
    void validationFailsInvalidToken() throws JwtService.ValidationException {
        JwtService service = new JwtService(AUDIENCE, ISSUER).withPublicKey(PUBLIC_KEY_STRING);
        var validationResponse = service.validateJwt("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTY5MDM0ODk5OSwic3ViIjoiVGVzdCIsImF1ZCI6Imh0dHBzOi8vb3B0b3V0LWludGVnLnVpZGFwaS5jb20iLCJvcGVyYXRvclZlcnNpb24iOiJWZXJzaW9uIDEuMiIsImVuY2xhdmVUeXBlIjoidGVzdCBlbmNsYXZlIHR5cGUiLCJyb2xlcyI6Ik9QRVJBVE9SLE9QVE9VVCIsImlzcyI6Imh0dHBzOi8vY29yZS1pbnRlZy51aWRhcGkuY29tIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoidGVzdCBlbmNsYXZlIGlkIn0.N9xYROMx2hnMIhtyyBLF-J13uWXpIU6jj_Tgufww6O8JBhrHHFliOF2xsPUcZ1sK6lGsmbHACwlPTRz8zhpKWKM0CMNjfiWHwBGykK32hDC321QEta0aX6utBAWIb1crb2JwZhPH1K0_4X-mxdiuxibgW3YNpQxm2kZDnQaR40py5JykVkPxzwhgzUCceDN5kL1kNEjnO");

        assertNotNull(validationResponse);
        assertFalse(validationResponse.getIsValid());
        assertNotNull(validationResponse.getValidationException());
    }

    @Test
    void getsPublicKeyFromKms() throws JwtService.ValidationException {

        JwtService service = new JwtService(AUDIENCE, ISSUER, Clock.systemUTC(), this.getBuilder(true, null)).withKmsKeyIdAndRegion("123", "ap-southeast-2");
        var validationResponse = service.validateJwt(VALID_TOKEN);

        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
        assertNull(validationResponse.getValidationException());
    }

    @Test
    void publicKeyReusedWithinExpiry() throws JwtService.ValidationException {
        JwtService service = new JwtService(AUDIENCE, ISSUER, Clock.systemUTC(), this.getBuilder(true, null)).withKmsKeyIdAndRegion("123", "ap-southeast-2");
        service.validateJwt(VALID_TOKEN);
        service.validateJwt(VALID_TOKEN);

        verify(mockClient, times(1)).getPublicKey((GetPublicKeyRequest) any());
    }

    @Test
    void throwsErrorIfNoKeyId() {
        JwtService service = new JwtService(AUDIENCE, ISSUER).withKmsKeyIdAndRegion("", "");
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN));
        assertEquals("KeyId or Public key must be specified", ex.getMessage());
    }
    @Test
    void throwsErrorIfInvalidPublicKey() {
        JwtService service = new JwtService(AUDIENCE, ISSUER).withPublicKey("Invalid key");
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN));
        assertEquals("Unable to get public key. Validation can not continue", ex.getMessage());
    }

    private KmsClientBuilder getBuilder(boolean isSuccessful, Optional<String> statusText) {
        SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.isSuccessful()).thenReturn(isSuccessful);
        when(sdkHttpResponse.statusText()).thenReturn(statusText);

        GetPublicKeyResponse response = mock(GetPublicKeyResponse.class);
        when(response.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(response.publicKey()).thenReturn(SdkBytes.fromByteArray(Base64.decode(COMPACT_PUBLIC_KEY)));

        mockClient = mock(KmsClient.class);
        capturedGetPublicKeyRequest = ArgumentCaptor.forClass(GetPublicKeyRequest.class);
        when(mockClient.getPublicKey(capturedGetPublicKeyRequest.capture())).thenReturn(response);

        KmsClientBuilder builder = mock(KmsClientBuilder.class);
        when(builder.region(any(Region.class))).thenReturn(builder);
        when(builder.credentialsProvider(any(AwsCredentialsProvider.class))).thenReturn(builder);
        when(builder.build()).thenReturn(mockClient);

        return builder;
    }

}
