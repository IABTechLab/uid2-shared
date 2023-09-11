package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Clock;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationException;

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

        tokenPayloadBuilder.dbgStat(TryGetField(rawPayload, "dbgstat", String.class));
        tokenPayloadBuilder.swName(TryGetField(rawPayload, "swname", String.class));
        var swVersion = TryGetField(rawPayload, "swversion", List.class);
        if(swVersion != null && !swVersion.isEmpty()){
            tokenPayloadBuilder.swVersion(TryConvert(swVersion.get(0), String.class));
        }

        var subModsDetails = TryGetField(rawPayload,"submods",  Map.class);

        if(subModsDetails != null){
            var confidential_space = TryGetField(subModsDetails, "confidential_space", Map.class);
            if(confidential_space != null){
                tokenPayloadBuilder.csSupportedAttributes(TryGetField(confidential_space, "support_attributes", List.class));
            }

            var container = TryGetField(subModsDetails, "container", Map.class);
            if(container != null){
                tokenPayloadBuilder.workloadImageReference(TryGetField(container, "image_reference", String.class));
                tokenPayloadBuilder.workloadImageDigest(TryGetField(container, "image_digest", String.class));
                tokenPayloadBuilder.restartPolicy(TryGetField(container, "restart_policy", String.class));

                tokenPayloadBuilder.cmdOverrides(TryGetField(container, "cmd_override", ArrayList.class));
                tokenPayloadBuilder.envOverrides(TryGetField(container, "env_override", Map.class));
            }

            var gce= TryGetField(subModsDetails, "gce", Map.class);
            if(gce != null){
                var gceZone = TryGetField(gce, "zone", String.class);
                tokenPayloadBuilder.gceZone(gceZone);
            }
        }

        return tokenPayloadBuilder.build();
    }

    private static<T> T TryGetField(Map payload, String key, Class<T> clazz){
        if(payload == null){
            return null;
        }
        var rawValue = payload.get(key);
        return TryConvert(rawValue, clazz);
    }

    private static<T> T TryConvert(Object obj, Class<T> clazz){
        if(obj == null){
            return null;
        }
        try{
            return clazz.cast(obj);
        }
        catch (ClassCastException e){
            return null;
        }
    }
}
