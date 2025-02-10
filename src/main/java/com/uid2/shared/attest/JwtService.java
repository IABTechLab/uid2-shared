package com.uid2.shared.attest;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import com.uid2.shared.Const;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private final JsonObject config;

    private final HashSet<PublicKey> publicKeys = new HashSet<>();


    public JwtService(JsonObject config) {
        this.config = config;
        String keysData = config.getString(Const.Config.AwsKmsJwtSigningPublicKeysProp, "");
        String[] keys = keysData.split(",");
        if (keysData.isBlank() || keys == null || keys.length == 0) {
            LOGGER.info("Unable to read public keys from the configuration. JWTs can not be verified.");
            return;
        }
        this.parsePublicKeysFromConfig(keys);
    }

    /*
     * Validates the jwt token signature is valid, and then has the expected
     * iss and aud values.
     * Checks the config value for the public keys. Will loop though all the keys in the config
     * so that when a key is rotated, or a new one give, we can still validate older tokens.
     */
    public JwtValidationResponse validateJwt(String jwt, String audience, String issuer) throws ValidationException {
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("Audience can not be empty");
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Issuer can not be empty");
        }

        JwtValidationResponse response = new JwtValidationResponse(false);

        if (this.publicKeys.isEmpty()) {
            LOGGER.error("Unable to get public keys. Validation can not continue. Check the configuration for the service and ensure all valid public keys are specified in the property: {}", Const.Config.AwsKmsJwtSigningPublicKeysProp);
            throw new ValidationException(Optional.of("Unable to get public keys. Validation can not continue"));
        }

        Exception lastException = null;

        for (PublicKey key : this.publicKeys) {
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
                        .withSubject(webToken.get("sub").toString())
                        .withRoles(webToken.get("roles").toString())
                        .withEnclaveId(webToken.get("enclaveId").toString())
                        .withEnclaveType(webToken.get("enclaveType").toString())
                        .withSiteId(Integer.valueOf(webToken.get("siteId").toString()))
                        .withOperatorVersion(webToken.get("operatorVersion").toString())
                        .withAudience(webToken.get("aud").toString());

                // return the first verified response
                return response;
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (!response.getIsValid()) {
            LOGGER.error("Error validating JWT", lastException);
        }

        response.setValidationException(lastException);
        return response;
    }

    private void parsePublicKeysFromConfig(String[] publicKeys) {
        Arrays.stream(publicKeys).forEach(key -> {
            try {
                if (key != null && !key.isBlank()) {
                    PublicKey publicKey = this.getPublicKeyFromString(key);
                    if (publicKey != null) {
                        this.publicKeys.add(publicKey);
                    }
                }
            } catch (ValidationException e) {
                LOGGER.error("Unable to parse Public Key string that starts with: {}", key.substring(0, key.length() > 15 ? 15 : key.length()));
            }
        });
    }

    private PublicKey getPublicKeyFromString(String keyString) throws ValidationException {
        try {
            String publicKeyPEM = keyString
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replaceAll("\n", "")
                    .replace("-----END PUBLIC KEY-----", "");
            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

            PublicKey key = keyFactory.generatePublic(keySpec);
            return key;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ex) {
            LOGGER.error("Error creating Public key from configuration:", ex);
            throw new ValidationException(Optional.of("Error creating Public key from configuration"), ex);
        }
    }

    public class ValidationException extends Exception {
        public ValidationException(Optional<String> message) {
            super(message == null ? "Validation Error" : message.orElse("Validation Error"));
        }
        public ValidationException(Optional<String> message, Exception ex) {
            super(message == null ? "Validation Error" : message.orElse("Validation Error"), ex);
        }
    }
}
