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
    private final static String VALID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjQ4NDgzNjAxNjgsImlhdCI6MTY5MjY4NjU3NCwic3ViIjoiS0FPUnhzUGRycDRqVnBQcWIwS2dwWWFsRjVSekJwZXAwZGR2UStsakYzdnltaFJYdjNDNkNJWERmaE5KV2RvYmN6bW9pZWU5R0kwMFZybHRMYm1xVUE9PSIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5MCIsIm9wZXJhdG9yVmVyc2lvbiI6IlNwZWNpYWx8dWlkMi1vcGVyYXRvcnwyLjcuMTYtU05BUFNIT1QiLCJlbmNsYXZlVHlwZSI6InRydXN0ZWQiLCJyb2xlcyI6Ik9QVE9VVCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4OCIsIm5hbWUiOiJTcGVjaWFsIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoiVHJ1c3RlZCJ9.ICbHPZnm25hIa69A06aNfduyh1xSdXDgxI0_ORGqdNdrop_6CCqk5HtM5uS2hJ_1opjNAjQdfE_CMaAfda13_bnOaZSPe-aQLQBx6vCEjYCRUaAvD7p4NgHnEz9u0nf-ijFBZh0MZR8MV2s3pcEcSbZ7rkSFYW73n2KuACDQxCRdl39wQ-Bs222pm0Q-BkCZlb6OFp6pd-SUT_VWHeSeWbSyXQpfald1WAmH1rHyUdWzfz005Jv8JCucgyorhInQZY0O7wQ-UPk5nuVqz0QkT7w2S4pa4gopLpsi0BM6gu5jDfQredy49v3wuuZRAiefw4ueGBevMA6RxAqtjGEAiA";
    private final static String EXPIRED_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjE2OTIzODE2MzMsImlhdCI6MTY5MjY3OTg3NSwic3ViIjoiZ1k1b1hUNmZaeG5xcVZhdFNHVnE4dU9xOW1tcVEvc1A4cU9SRzZ5UStraUZyNHJCQWR6SFBORXpQTXFxK3JlNFEvb1ovaXFqa0IvL3dBLzAxT2w3RkE9PSIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5MCIsIm9wZXJhdG9yVmVyc2lvbiI6IlNwZWNpYWwgKE9sZCwgUHJpdmF0ZSl8dWlkMi1vcGVyYXRvcnwyLjcuMTYtU05BUFNIT1QiLCJlbmNsYXZlVHlwZSI6InRydXN0ZWQiLCJyb2xlcyI6Ik9QVE9VVCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4OCIsIm5hbWUiOiJTcGVjaWFsIChPbGQsIFByaXZhdGUpIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoiVHJ1c3RlZCJ9.gRdwQqNc7IHuPBSVjtjuBoEpr4n6vwZIFh0LbW1cqeYZd7uORYyizLyyPtG_Mh_A73LoiYQ4Rn0q1VnMdBqIqsDFYILP182XGNvsToTj_HlpP95DbKs7RfdZ7XTxKflG00M156xdJmieY_v1OQiOjw3vYt82F4QUOaIm5M_k7TAs6GgYPmhinILIJWWgljk7reGmEuWl_abU0wU4cQilwL9F9Byto5Ba8CHLAmv62PcsjbuU8LN-RrkCTYzgIqUUdRceIyrfnxP4UgKqijrjIMbeVvYyTY43anQHicoRYZCQXMaUQWk57xipFTCc-J6QKNPvh198PqtuBUBbg073Mw";
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
    }

    @Test
    void validationFailsInvalidToken() {
        this.addPublicKeysToConfig(PUBLIC_KEY_STRING, COMPACT_PUBLIC_KEY);
        JwtService service = new JwtService(config);
        assertThrows(JwtService.ValidationException.class, () ->
            service.validateJwt("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTY5MDM0ODk5OSwic3ViIjoiVGVzdCIsImF1ZCI6Imh0dHBzOi8vb3B0b3V0LWludGVnLnVpZGFwaS5jb20iLCJvcGVyYXRvclZlcnNpb24iOiJWZXJzaW9uIDEuMiIsImVuY2xhdmVUeXBlIjoidGVzdCBlbmNsYXZlIHR5cGUiLCJyb2xlcyI6Ik9QRVJBVE9SLE9QVE9VVCIsImlzcyI6Imh0dHBzOi8vY29yZS1pbnRlZy51aWRhcGkuY29tIiwic2l0ZUlkIjoiOTk5IiwiZW5jbGF2ZUlkIjoidGVzdCBlbmNsYXZlIGlkIn0.N9xYROMx2hnMIhtyyBLF-J13uWXpIU6jj_Tgufww6O8JBhrHHFliOF2xsPUcZ1sK6lGsmbHACwlPTRz8zhpKWKM0CMNjfiWHwBGykK32hDC321QEta0aX6utBAWIb1crb2JwZhPH1K0_4X-mxdiuxibgW3YNpQxm2kZDnQaR40py5JykVkPxzwhgzUCceDN5kL1kNEjnO", AUDIENCE, ISSUER)
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
