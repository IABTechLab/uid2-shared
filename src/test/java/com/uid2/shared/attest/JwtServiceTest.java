package com.uid2.shared.attest;

import com.amazonaws.util.Base64;
import com.uid2.shared.Const;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    // The token was generated using the private key: https://github.com/IABTechLab/uid2-admin/blob/main/src/main/resources/localstack/kms/seed.yaml#L9
    private final static String VALID_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjQ4NDgzNjAxNjgsImlhdCI6MTY5MjY4NjU3NCwic3ViIjoiS0FPUnhzUGRycDRqVnBQcWIwS2dwWWFsRjVSekJwZXAwZGR2UStsakYzdnltaFJYdjNDNkNJWERmaE5KV2RvYmN6bW9pZWU5R0kwMFZybHRMYm1xVUE9PSIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5MCIsIm9wZXJhdG9yVmVyc2lvbiI6IlNwZWNpYWx8dWlkMi1vcGVyYXRvcnwyLjcuMTYtU05BUFNIT1QiLCJlbmNsYXZlVHlwZSI6InRydXN0ZWQiLCJyb2xlcyI6Ik9QVE9VVCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4OCIsIm5hbWUiOiJTcGVjaWFsIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoiVHJ1c3RlZCIsImp0aSI6ImR1bW15SnRpIn0.MLfb17MtIay-RzDEmrzC4gUHyNAXpBIFJai1-aiFIOmrejInqKDbYZdgnSAbPwHWPgExvr68dyCf362SZqjw3YyUDx6snQ47o3gAqrc0_reizkmgQ_xaa5useayg-8b8Csu9C7XaCoCo0c_Lq9pid34Kmx_LtC6l8rDmyY78AbiecdN_H90BmfCgm1-4QM4gGfjfKxyvkdPioYziAEw4QQ6X2SFiPMl36PCrHkc8UE0jgaI1xCYFghZTBJ0BonIOVoGYEZld-s9OIWtMWx58HYFXygIRLMS0y5_bDq2JCiQx7KNJtckUh8E3PGkaQAHoWjHf9URSusrM_R06tJRxYQ";
    private final static String EXPIRED_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2OTIzODE2MzMsImlhdCI6MTY5MjY3OTg3NSwic3ViIjoiZ1k1b1hUNmZaeG5xcVZhdFNHVnE4dU9xOW1tcVEvc1A4cU9SRzZ5UStraUZyNHJCQWR6SFBORXpQTXFxK3JlNFEvb1ovaXFqa0IvL3dBLzAxT2w3RkE9PSIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5MCIsIm9wZXJhdG9yVmVyc2lvbiI6IlNwZWNpYWwgKE9sZCwgUHJpdmF0ZSl8dWlkMi1vcGVyYXRvcnwyLjcuMTYtU05BUFNIT1QiLCJlbmNsYXZlVHlwZSI6InRydXN0ZWQiLCJyb2xlcyI6Ik9QVE9VVCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4OCIsIm5hbWUiOiJTcGVjaWFsIChPbGQsIFByaXZhdGUpIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoiVHJ1c3RlZCIsImp0aSI6ImR1bW15SnRpIn0.NMJPbN_hCGkxWK9ZEBhsHKHxdXvElCDsgph4NBiMNtGVe-Yw7nZxYDpyw9cvEws9EPckMgDXfOGo9SqOhHbsapB0-JDLVebHk2yXpzft1DRF7zoY7MLXdoV5-apZDeRmJyuCAtGVpKpP03CoJ-8Dd9FETmk8x4GE0JktkKi1O7WnyNv7g8bqI8IR-VCi83sjTeQrQSBj1sV0VCYPIcg5dFcgApiYt_b2u7ugM0B090IsKK-ChigC6jKHqKLlRT5OlvnR5y7q9D37MIgsDQQbqRNaAsWzFjk1HA1iHTodNO4FlWyYJsrpC_XBnZzqqu2YLQ00cSMO2i--Cycjgmvt5A";
    private final static String INVALID_TOKEN = "invalid_token";
    private final static String PUBLIC_KEY_STRING = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmvwB41qI5Fe41PDbXqcX\n" +
            "5uOvSvfKh8l9QV0O3M+NsB4lKqQEP0t1hfoiXTpOgKz1ArYxHsQ2LeXifX4uwEbY\n" +
            "JFlpVM+tyQkTWQjBOw6fsLYK2Xk4X2ylNXUUf7x3SDiOVxyvTh3OZW9kqrDBN9Jx\n" +
            "SoraNLyfw0hhW0SHpfs699SehgbQ7QWep/gVlKRLIz0XAXaZNw24s79ORcQlrCE6\n" +
            "YD0PgQmpI/dK5xMML82n6y3qcTlywlGaU7OGIMdD+CTXA3BcOkgXeqZTXNaX1u6j\n" +
            "CTa1lvAczun6avp5VZ4TFiuPo+y4rJ3GU+14cyT5NckEcaTKSvd86UdwK5Id9tl3\n" +
            "bQIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";

    private final static String COMPACT_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmvwB41qI5Fe41PDbXqcX5uOvSvfKh8l9QV0O3M+NsB4lKqQEP0t1hfoiXTpOgKz1ArYxHsQ2LeXifX4uwEbYJFlpVM+tyQkTWQjBOw6fsLYK2Xk4X2ylNXUUf7x3SDiOVxyvTh3OZW9kqrDBN9JxSoraNLyfw0hhW0SHpfs699SehgbQ7QWep/gVlKRLIz0XAXaZNw24s79ORcQlrCE6YD0PgQmpI/dK5xMML82n6y3qcTlywlGaU7OGIMdD+CTXA3BcOkgXeqZTXNaX1u6jCTa1lvAczun6avp5VZ4TFiuPo+y4rJ3GU+14cyT5NckEcaTKSvd86UdwK5Id9tl3bQIDAQAB";

    private final static String AUDIENCE = "http://localhost:8090"; // e2e Optout URL
    private final static String ISSUER = "http://localhost:8088"; // e2e Core URL

    private JsonObject config;

    @BeforeEach
    void setUp() {
        this.config = new JsonObject();
    }

    private void addPublicKeysToConfig(String... keys) {
        if (keys != null) {
            String keysArray = String.join(",", keys);
            this.config.put(Const.Config.AwsKmsJwtSigningPublicKeysProp, keysArray);
        }
    }

    @Test
    void validateTokenSucceeds() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);
        var validationResponse = assertDoesNotThrow(() ->
            service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER)
        );
        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
        assertEquals(AUDIENCE, validationResponse.getAudience());
        assertEquals("Trusted", validationResponse.getEnclaveId());
        assertEquals("trusted", validationResponse.getEnclaveType());
        assertEquals(999, validationResponse.getSiteId());
        assertEquals("Special|uid2-operator|2.7.16-SNAPSHOT", validationResponse.getOperatorVersion());
        assertEquals("dummyJti", validationResponse.getJti());
    }

    @Test
    void validationFailsInvalidToken() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);
        assertThrows(JwtService.ValidationException.class, () ->
            service.validateJwt(INVALID_TOKEN, AUDIENCE, ISSUER)
        );
    }

    @Test
    void validationFailsExpiredToken() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);
        var ex = assertThrows(JwtService.ValidationException.class, () ->
            service.validateJwt(EXPIRED_TOKEN, AUDIENCE, ISSUER)
        );
        assertEquals("Token is expired", ex.getMessage());
    }

    @Test
    void throwsErrorIfNoPublicKeys() {
        JwtService service = new JwtService(config);
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
        assertEquals("Unable to get public keys. Validation can not continue", ex.getMessage());
    }

    @Test
    void throwsErrorIfInvalidPublicKey() {
        this.addPublicKeysToConfig("Invalid key");
        JwtService service = new JwtService(config);
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
        assertEquals("Unable to get public keys. Validation can not continue", ex.getMessage());
    }

    @Test
    void validateTokenSucceedsSecondPublicKeyValid() {
        this.addPublicKeysToConfig("Invalid key", COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);

        var validationResponse = assertDoesNotThrow(() ->
            service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER)
        );
        assertNotNull(validationResponse);
        assertTrue(validationResponse.getIsValid());
    }

    @Test
    void validateTokenEmptyAudienceThrows() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.validateJwt(VALID_TOKEN, null, ISSUER));
        assertEquals("Audience can not be empty", ex.getMessage());
    }
    @Test
    void validateTokenEmptyIssuerThrows() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, null));
        assertEquals("Issuer can not be empty", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void nullPublicKeysPasses(String key) {
        this.addPublicKeysToConfig(key);
        JwtService service = new JwtService(config);
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
        assertEquals("Unable to get public keys. Validation can not continue", ex.getMessage());
    }
    @Test
    void noPublicKeysConfigPasses() {
        JwtService service = new JwtService(config);
        var ex = assertThrows(JwtService.ValidationException.class, () -> service.validateJwt(VALID_TOKEN, AUDIENCE, ISSUER));
        assertEquals("Unable to get public keys. Validation can not continue", ex.getMessage());
    }}
