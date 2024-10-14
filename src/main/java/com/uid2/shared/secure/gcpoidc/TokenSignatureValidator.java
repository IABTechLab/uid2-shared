package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Clock;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationFailure;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.uid2.shared.secure.JwtUtils.tryConvert;
import static com.uid2.shared.secure.JwtUtils.tryGetField;

public class TokenSignatureValidator implements ITokenSignatureValidator {
    private static final String PUBLIC_CERT_LOCATION =
            "https://www.googleapis.com/service_accounts/v1/metadata/jwk/signer@confidentialspace-sign.iam.gserviceaccount.com";

    // TODO: update audience once GCP supports customized claims
    private static final String AUDIENCE = "https://sts.googleapis.com";
    private static final String ISSUER = "https://confidentialcomputing.googleapis.com";
    private final TokenVerifier tokenVerifier;

    // set to true to facilitate local test with self-signed cert.
    public static final boolean BYPASS_SIGNATURE_CHECK = false;

    public TokenSignatureValidator() {
        this(null, null);
    }

    protected TokenSignatureValidator(PublicKey publicKeyOverride, Clock clockOverride) {
        var verifierBuilder = TokenVerifier.newBuilder();
        verifierBuilder.setCertificatesLocation(PUBLIC_CERT_LOCATION);

        if (publicKeyOverride != null) {
            verifierBuilder.setPublicKey(publicKeyOverride);
        }

        if (clockOverride != null) {
            verifierBuilder.setClock(clockOverride);
        }

        verifierBuilder.setAudience(AUDIENCE);
        verifierBuilder.setIssuer(ISSUER);

        tokenVerifier = verifierBuilder.build();
    }

    @Override
    public TokenPayload validate(String tokenString) throws AttestationClientException {
        if (Strings.isNullOrEmpty(tokenString)) {
            throw new IllegalArgumentException("tokenString can not be null or empty");
        }

        // Validate Signature
        JsonWebSignature signature;
        try {
            if (BYPASS_SIGNATURE_CHECK) {
                signature = JsonWebSignature.parse(GsonFactory.getDefaultInstance(), tokenString);
            } else {
                signature = tokenVerifier.verify(tokenString);
            }
        } catch (TokenVerifier.VerificationException e) {
            throw new AttestationClientException("Fail to validate the token signature, error: " + e.getMessage(), AttestationFailure.BAD_CERTIFICATE);
        } catch (IOException e) {
            throw new AttestationClientException("Fail to parse token, error: " + e.getMessage(), AttestationFailure.BAD_PAYLOAD);
        }

        // Parse Payload
        var rawPayload = signature.getPayload();

        var tokenPayloadBuilder = TokenPayload.builder();

        tokenPayloadBuilder.dbgStat(tryGetField(rawPayload, "dbgstat", String.class));
        tokenPayloadBuilder.swName(tryGetField(rawPayload, "swname", String.class));
        var swVersion = tryGetField(rawPayload, "swversion", List.class);
        if (swVersion != null && !swVersion.isEmpty()) {
            tokenPayloadBuilder.swVersion(tryConvert(swVersion.get(0), String.class));
        }

        var subModsDetails = tryGetField(rawPayload, "submods", Map.class);

        if (subModsDetails != null) {
            var confidential_space = tryGetField(subModsDetails, "confidential_space", Map.class);
            if (confidential_space != null) {
                tokenPayloadBuilder.csSupportedAttributes(tryGetField(confidential_space, "support_attributes", List.class));
            }

            var container = tryGetField(subModsDetails, "container", Map.class);
            if (container != null) {
                tokenPayloadBuilder.workloadImageReference(tryGetField(container, "image_reference", String.class));
                tokenPayloadBuilder.workloadImageDigest(tryGetField(container, "image_digest", String.class));
                tokenPayloadBuilder.restartPolicy(tryGetField(container, "restart_policy", String.class));

                tokenPayloadBuilder.cmdOverrides(tryGetField(container, "cmd_override", ArrayList.class));
                tokenPayloadBuilder.envOverrides(tryGetField(container, "env_override", Map.class));
            }

            var gce = tryGetField(subModsDetails, "gce", Map.class);
            if (gce != null) {
                var gceZone = tryGetField(gce, "zone", String.class);
                tokenPayloadBuilder.gceZone(gceZone);
            }
        }

        return tokenPayloadBuilder.build();
    }
}
