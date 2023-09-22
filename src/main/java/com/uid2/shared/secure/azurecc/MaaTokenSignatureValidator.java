package com.uid2.shared.secure.azurecc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Clock;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.JwtUtils;
import com.uid2.shared.secure.gcpoidc.TokenPayload;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Map;

public class MaaTokenSignatureValidator implements IMaaTokenSignatureValidator {

    // set to true to facilitate local test.
    public static final boolean BYPASS_SIGNATURE_CHECK = false;

    // e.g. https://sharedeus.eus.attest.azure.net
    private final String maaServerBaseUrl;

    // used in UT
    private final PublicKey publicKeyOverride;
    private final Clock clockOverride;
    private final AzurePublicKeyProvider publicKeyProvider;

    public MaaTokenSignatureValidator(String maaServerBaseUrl) {
        this(maaServerBaseUrl, null, null);
    }

    protected MaaTokenSignatureValidator(String maaServerBaseUrl, PublicKey publicKeyOverride, Clock clockOverride) {
        this.maaServerBaseUrl = maaServerBaseUrl;
        this.publicKeyOverride = publicKeyOverride;
        this.clockOverride = clockOverride;
        this.publicKeyProvider = new AzurePublicKeyProvider();
    }

    private TokenVerifier buildTokenVerifier(String kid) throws AttestationException {
        var verifierBuilder = TokenVerifier.newBuilder();

        if (publicKeyOverride != null) {
            verifierBuilder.setPublicKey(publicKeyOverride);
        }
        else{
            verifierBuilder.setPublicKey(publicKeyProvider.GetPublicKey(maaServerBaseUrl, kid));
        }

        if (clockOverride != null) {
            verifierBuilder.setClock(clockOverride);
        }

        verifierBuilder.setIssuer(maaServerBaseUrl);

        return verifierBuilder.build();
    }

    @Override
    public MaaTokenPayload validate(String tokenString) throws AttestationException {
        if (Strings.isNullOrEmpty(tokenString)) {
            throw new IllegalArgumentException("tokenString can not be null or empty");
        }

        // Validate Signature
        JsonWebSignature signature;
        try {
            signature = JsonWebSignature.parse(GsonFactory.getDefaultInstance(), tokenString);
            if(!BYPASS_SIGNATURE_CHECK){
                var kid = signature.getHeader().getKeyId();
                var tokenVerifier = buildTokenVerifier(kid);
                tokenVerifier.verify(tokenString);
            }
        } catch (TokenVerifier.VerificationException e) {
            throw new AttestationException("Fail to validate the token signature, error: " + e.getMessage());
        } catch (IOException e) {
            throw new AttestationException("Fail to parse token, error: " + e.getMessage());
        }

        // Parse Payload
        var rawPayload = signature.getPayload();

        var tokenPayloadBuilder = MaaTokenPayload.builder();

        tokenPayloadBuilder.attestationType(JwtUtils.tryGetField(rawPayload, "x-ms-attestation-type", String.class));
        tokenPayloadBuilder.complianceStatus(JwtUtils.tryGetField(rawPayload, "x-ms-compliance-status", String.class));
        tokenPayloadBuilder.vmDebuggable(JwtUtils.tryGetField(rawPayload, "x-ms-sevsnpvm-is-debuggable", Boolean.class));
        tokenPayloadBuilder.ccePolicy(JwtUtils.tryGetField(rawPayload, "x-ms-sevsnpvm-hostdata", String.class));

        var  runtime = JwtUtils.tryGetField(rawPayload, ("x-ms-runtime"), Map.class);

        if(runtime != null){
            var runtimeDataBuilder = RuntimeData.builder();
            runtimeDataBuilder.location(JwtUtils.tryGetField(runtime, "location", String.class));
            runtimeDataBuilder.publicKey(JwtUtils.tryGetField(runtime, "publicKey", String.class));
            tokenPayloadBuilder.runtimeData(runtimeDataBuilder.build());
        }

        return tokenPayloadBuilder.build();
    }
}
