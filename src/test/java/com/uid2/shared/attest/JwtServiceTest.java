package com.uid2.shared.attest;

import com.amazonaws.util.Base64;
import com.uid2.shared.Const;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
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
    private final static String EXPIRED_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjk0NjY4NDgwMCwiaWF0IjoxNjkwNDE5MjczLCJzdWIiOiJUZXN0IiwiYXVkIjoiaHR0cHM6Ly9vcHRvdXQtaW50ZWcudWlkYXBpLmNvbSIsIm9wZXJhdG9yVmVyc2lvbiI6IlZlcnNpb24gMS4yIiwiZW5jbGF2ZVR5cGUiOiJ0ZXN0IGVuY2xhdmUgdHlwZSIsInJvbGVzIjoiT1BFUkFUT1IsT1BUT1VUIiwiaXNzIjoiaHR0cHM6Ly9jb3JlLWludGVnLnVpZGFwaS5jb20iLCJzaXRlSWQiOiI5OTkiLCJlbmNsYXZlSWQiOiJ0ZXN0IGVuY2xhdmUgaWQifQ.YVZcIDTxJMLonFffK4gKCYRRIAYTfSFwbeYcmXethoGt96TieajFMzy9TmEo7KdoLfRQCRKhdGD0TLU16v8euS2M151x0I97lrUujXC2zYlgXARS3YwdxatcvieQ_1NTIPr2Lg-y8BTScLmT65IFqQXiVz4dnYbyEmvdVBLxy-49XKW0CapApLE1TBNHMs8ktRVu61v_H9fpiZ2A0L8KzjgIgYeHYRTjvOL5cH8yeRGioZE7WLLGphz6STLgx6qrT5Z2i4IoVHUY8bPmj0EwQXMPuGCr3ds9gabUmTgpNMcR30sr2VFsz0TPDcVfv6vaXpEtztlzdx_umWayMO1ALA";
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
    private JsonObject config;

    @BeforeEach
    void setUp() {
        this.config = new JsonObject();
    }

    @Test
    void validateTokenSucceeds() throws JwtService.ValidationException {
        config.put(Const.Config.AwsKmsJwtSigningPublicKeyProp, PUBLIC_KEY_STRING);
        JwtService service = new JwtService(config);
        var validationResponse = service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER);

        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
        assertNull(validationResponse.getValidationException());
    }

    @Test
    void validationFailsInvalidToken() throws JwtService.ValidationException {
        config.put(Const.Config.AwsKmsJwtSigningPublicKeyProp, PUBLIC_KEY_STRING);
        JwtService service = new JwtService(config);
        var validationResponse = service.validateJwt("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTY5MDM0ODk5OSwic3ViIjoiVGVzdCIsImF1ZCI6Imh0dHBzOi8vb3B0b3V0LWludGVnLnVpZGFwaS5jb20iLCJvcGVyYXRvclZlcnNpb24iOiJWZXJzaW9uIDEuMiIsImVuY2xhdmVUeXBlIjoidGVzdCBlbmNsYXZlIHR5cGUiLCJyb2xlcyI6Ik9QRVJBVE9SLE9QVE9VVCIsImlzcyI6Imh0dHBzOi8vY29yZS1pbnRlZy51aWRhcGkuY29tIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoidGVzdCBlbmNsYXZlIGlkIn0.N9xYROMx2hnMIhtyyBLF-J13uWXpIU6jj_Tgufww6O8JBhrHHFliOF2xsPUcZ1sK6lGsmbHACwlPTRz8zhpKWKM0CMNjfiWHwBGykK32hDC321QEta0aX6utBAWIb1crb2JwZhPH1K0_4X-mxdiuxibgW3YNpQxm2kZDnQaR40py5JykVkPxzwhgzUCceDN5kL1kNEjnO", AUDIENCE, ISSUER);

        assertNotNull(validationResponse);
        assertFalse(validationResponse.getIsValid());
        assertNotNull(validationResponse.getValidationException());
    }
    @Test
    void validationFailsExpiredToken() throws JwtService.ValidationException {
        config.put(Const.Config.AwsKmsJwtSigningPublicKeyProp, PUBLIC_KEY_STRING);
        JwtService service = new JwtService(config);
        var validationResponse = service.validateJwt(EXPIRED_TOKEN, AUDIENCE, ISSUER);

        assertNotNull(validationResponse);
        assertFalse(validationResponse.getIsValid());
        assertNotNull(validationResponse.getValidationException());
        assertEquals("Token is expired", validationResponse.getValidationException().getMessage());
    }

    @Test
    void getsPublicKeyFromKms() throws JwtService.ValidationException {
        config.put(Const.Config.AwsKmsJwtSigningKeyIdProp, "123");
        config.put(Const.Config.AwsRegionProp, "ap-southeast-2");

        JwtService service = new JwtService(config, Clock.systemUTC(), this.getBuilder(true, null));
        var validationResponse = service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER);

        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
        assertNull(validationResponse.getValidationException());
        assertEquals("123", capturedGetPublicKeyRequest.getValue().keyId());

    }

    @Test
    void publicKeyReusedWithinExpiry() throws JwtService.ValidationException {
        config.put(Const.Config.AwsKmsJwtSigningKeyIdProp, "123");
        config.put(Const.Config.AwsRegionProp, "ap-southeast-2");

        JwtService service = new JwtService(config, Clock.systemUTC(), this.getBuilder(true, null));
        service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER);
        service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER);

        verify(mockClient, times(1)).getPublicKey((GetPublicKeyRequest) any());
    }

    @Test
    void throwsErrorIfNoKeyId() {
        config.put(Const.Config.AwsKmsJwtSigningKeyIdProp, "");
        config.put(Const.Config.AwsRegionProp, "");

        JwtService service = new JwtService(config, Clock.systemUTC(), this.getBuilder(true, null));
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
        assertEquals("KeyId or Public key must be specified", ex.getMessage());
    }
    @Test
    void throwsErrorIfInvalidPublicKey() {
        config.put(Const.Config.AwsKmsJwtSigningPublicKeyProp, "Invalid key");
        JwtService service = new JwtService(config);

        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
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
