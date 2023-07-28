package com.uid2.shared.attest;

import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudUtils;
import io.vertx.core.json.JsonObject;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.kms.model.*;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtService {

    private static final Integer cacheIntervalInSeconds = 3600;
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private PublicKey cachedPublicKey;
    private Instant nextCacheRefresh;
    private final JsonObject config;
    private final Clock clock;
    private final KmsClientBuilder kmsClientBuilder;
    private String localPublicKey;
    private String kmsKeyId;
    private String kmsRegion;

    private KmsClient kmsClient = null;

    public JwtService(JsonObject config) {
        this(config, Clock.systemUTC(), KmsClient.builder());
    }

    public JwtService(JsonObject config, Clock clock, KmsClientBuilder kmsClientBuilder) {
        this.config = config;
        this.clock = clock;
        this.kmsClientBuilder = kmsClientBuilder;
        String publicKey = config.getString(Const.Config.AwsKmsJwtSigningPublicKeyProp);
        if (publicKey != null && !publicKey.isEmpty()) {
            this.setPublicKey(publicKey);
        } else {
            this.setKmsKeyIdAndRegion(config.getString(Const.Config.AwsKmsJwtSigningKeyIdProp), config.getString(Const.Config.AwsRegionProp));
        }
    }

    public JwtService setPublicKey(String publicKey) {
        if (publicKey != null && !publicKey.isEmpty() && this.localPublicKey != publicKey) {
            cachedPublicKey = null;
            nextCacheRefresh = this.clock.instant();
            this.localPublicKey = publicKey;
        }
        return this;
    }

    public JwtService setKmsKeyIdAndRegion(String kmsKeyId, String region) {
        if (kmsKeyId != null && !kmsKeyId.isEmpty()
            && region != null && !region.isEmpty()
            && (this.kmsKeyId != kmsKeyId || this.kmsRegion != region)) {
            cachedPublicKey = null;
            nextCacheRefresh = this.clock.instant();
            this.kmsKeyId = kmsKeyId;
            this.kmsRegion = region;
        }

        return this;
    }

    /*
     * Validates the jwt token signature is valid, and then has the expected
     * iss and aud values.
     * Checks the config value for the public key first. If this is set, uses that value for validation
     *      If the public key is set and validation fails, will NOT try a different key
     * If the public key property is not set, looks for the AWS Key Id.
     * If the Key Id is set, retrieves the public key from AWS and then caches that. The key is cached for 60 minutes.
     * If the Key Id is set, and validation fails, will try once to retrieve the public key again from AWS to validate
     * This is so that if they key is rotated, we don't have to wait for the cache expiry before validation passes
     */
    public JwtValidationResponse validateJwt(String jwt, String audience, String issuer) throws ValidationException {
        JwtValidationResponse response = new JwtValidationResponse(false);

        PublicKey key = this.getPublicKey();
        if (key == null) {
            LOGGER.error("Error getting public key. Token validation can not continue");
            throw new ValidationException(Optional.of("Unable to get public key. Validation can not continue"));
        }

        var tokenVerifier = TokenVerifier.newBuilder()
                .setPublicKey(key)
                .setAudience(audience)
                .setIssuer(issuer)
                .build();

        try {
            // verify checks that the token has not expired
            JsonWebSignature signature = tokenVerifier.verify(jwt);
            JsonWebToken.Payload webToken = signature.getPayload();
            response = new JwtValidationResponse(true)
                    .withRoles(webToken.get("roles").toString())
                    .withEnclaveId(webToken.get("enclaveId").toString())
                    .withEnclaveType(webToken.get("enclaveType").toString())
                    .withSiteId(Integer.valueOf(webToken.get("siteId").toString()))
                    .withOperatorVersion(webToken.get("operatorVersion").toString());

        } catch (TokenVerifier.VerificationException e) {
            LOGGER.info("Error validating JWT", e);
            response.setValidationException(e);
        } catch (Exception e) {
            LOGGER.warn("Error thrown verifying token", e);
            response.setValidationException(e);
        }

        return response;
    }

    // Gets the public key. Checks config first, and if that is set, tries to use that. If that fails to be parsed, does not try the KMS.
    // If the cache has been set and has not expired, returns the cached key.
    // Otherwise calls KMS to get the key, and caches the result.
    private PublicKey getPublicKey() throws ValidationException {
        if (this.localPublicKey != null && !this.localPublicKey.isEmpty()) {
            try {
                if (cachedPublicKey == null) {
                    String publicKeyPEM = this.localPublicKey
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replaceAll(System.lineSeparator(), "")
                            .replaceAll("\n", "")
                            .replace("-----END PUBLIC KEY-----", "");
                    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

                    PublicKey key = keyFactory.generatePublic(keySpec);
                    this.cachePublicKey(key);
                }

                return cachedPublicKey;
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ex) {
                LOGGER.error("Error reading Public key from configuration:", ex);
                return null;
            }
        }

        return getPublicKeyFromKmsOrCache();
    }

    private PublicKey getPublicKeyFromKmsOrCache() throws ValidationException {
        if (cachedPublicKey != null && nextCacheRefresh.isAfter(this.clock.instant())) {
            return cachedPublicKey;
        }

        return getPublicKeyFromKms();
    }

    private PublicKey getPublicKeyFromKms() throws ValidationException {
        try {
            if (this.kmsKeyId == null || this.kmsKeyId.isEmpty()) {
                throw  new ValidationException(Optional.of("KeyId or Public key must be specified"));
            }
            LOGGER.info("Refreshing the public key for JWT validation");

            GetPublicKeyRequest request = GetPublicKeyRequest
                    .builder()
                    .keyId(this.kmsKeyId)
                    .build();

            if (this.kmsClient == null) {
                this.kmsClient = CloudUtils.getKmsClient(this.kmsClientBuilder, this.config);
            }

            var response = this.kmsClient.getPublicKey(request);
            if (response.sdkHttpResponse().isSuccessful()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(response.publicKey().asByteArray());
                PublicKey key = keyFactory.generatePublic(keySpec);
                this.cachePublicKey(key);
                return cachedPublicKey;
            } else {
                LOGGER.error("Error response from AWS KMS: Response Code: {}, Response Text: {}", response.sdkHttpResponse().statusCode(), response.sdkHttpResponse().statusText());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | URISyntaxException e) {
            LOGGER.error("Error getting Public Key from KMS", e);
        }
        return null;
    }

    private void cachePublicKey(PublicKey key) {
        cachedPublicKey = key;
        nextCacheRefresh = this.clock.instant().plusSeconds(cacheIntervalInSeconds);
    }

    public class ValidationException extends Exception {
        public ValidationException(Optional<String> message) {
            super(message == null ? "Validation Error" : message.orElse("Validation Error"));
        }
    }
}
