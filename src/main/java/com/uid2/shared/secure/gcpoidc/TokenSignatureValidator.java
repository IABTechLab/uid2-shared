package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Clock;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.JwtUtils;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public TokenPayload validate(String tokenString) throws AttestationException {
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
            throw new AttestationException("Fail to validate the token signature, error: " + e.getMessage());
        } catch (IOException e) {
            throw new AttestationException("Fail to parse token, error: " + e.getMessage());
        }

        // Parse Payload
        var rawPayload = signature.getPayload();

        var tokenPayloadBuilder = TokenPayload.builder();

        tokenPayloadBuilder.dbgStat(JwtUtils.tryGetField(rawPayload, "dbgstat", String.class));
        tokenPayloadBuilder.swName(JwtUtils.tryGetField(rawPayload, "swname", String.class));
        var swVersion = JwtUtils.tryGetField(rawPayload, "swversion", List.class);
        if(swVersion != null && !swVersion.isEmpty()){
            tokenPayloadBuilder.swVersion(JwtUtils.tryConvert(swVersion.get(0), String.class));
        }

        var subModsDetails = JwtUtils.tryGetField(rawPayload,"submods",  Map.class);

        if(subModsDetails != null){
            var confidential_space = JwtUtils.tryGetField(subModsDetails, "confidential_space", Map.class);
            if(confidential_space != null){
                tokenPayloadBuilder.csSupportedAttributes(JwtUtils.tryGetField(confidential_space, "support_attributes", List.class));
            }

            var container = JwtUtils.tryGetField(subModsDetails, "container", Map.class);
            if(container != null){
                tokenPayloadBuilder.workloadImageReference(JwtUtils.tryGetField(container, "image_reference", String.class));
                tokenPayloadBuilder.workloadImageDigest(JwtUtils.tryGetField(container, "image_digest", String.class));
                tokenPayloadBuilder.restartPolicy(JwtUtils.tryGetField(container, "restart_policy", String.class));

                tokenPayloadBuilder.cmdOverrides(JwtUtils.tryGetField(container, "cmd_override", ArrayList.class));
                tokenPayloadBuilder.envOverrides(JwtUtils.tryGetField(container, "env_override", Map.class));
            }

            var gce = JwtUtils.tryGetField(subModsDetails, "gce", Map.class);
            if(gce != null){
                var gceZone = JwtUtils.tryGetField(gce, "zone", String.class);
                tokenPayloadBuilder.gceZone(gceZone);
            }
        }

        return tokenPayloadBuilder.build();
    }


}
