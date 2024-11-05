package com.uid2.shared.secure.azurecc;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Clock;
import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Strings;
import com.uid2.shared.secure.AttestationClientException;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.AttestationFailure;

import java.io.IOException;
import java.util.Map;

import static com.uid2.shared.secure.JwtUtils.tryGetField;

public class MaaTokenSignatureValidator implements IMaaTokenSignatureValidator {

    // set to true to facilitate local test.
    public static final boolean BYPASS_SIGNATURE_CHECK = false;

    // e.g. https://sharedeus.eus.attest.azure.net
    private final String maaServerBaseUrl;

    private final IPublicKeyProvider publicKeyProvider;

    // used in UT
    private final Clock clockOverride;

    public MaaTokenSignatureValidator(String maaServerBaseUrl) {
        this(maaServerBaseUrl, new AzurePublicKeyProvider(), null);
    }

    protected MaaTokenSignatureValidator(String maaServerBaseUrl, IPublicKeyProvider publicKeyProvider, Clock clockOverride) {
        this.maaServerBaseUrl = maaServerBaseUrl;
        this.publicKeyProvider = publicKeyProvider;
        this.clockOverride = clockOverride;
    }

    private TokenVerifier buildTokenVerifier(String kid) throws AttestationException {
        var verifierBuilder = TokenVerifier.newBuilder();

        verifierBuilder.setPublicKey(publicKeyProvider.GetPublicKey(maaServerBaseUrl, kid));

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
            throw new AttestationClientException("Fail to validate the token signature, error: " + e.getMessage(), AttestationFailure.BAD_PAYLOAD);
        } catch (IOException e) {
            throw new AttestationClientException("Fail to parse token, error: " + e.getMessage(), AttestationFailure.BAD_PAYLOAD);
        }

        // Parse Payload
        var rawPayload = signature.getPayload();

        var tokenPayloadBuilder = MaaTokenPayload.builder();

        tokenPayloadBuilder.attestationType(tryGetField(rawPayload, "x-ms-attestation-type", String.class));
        tokenPayloadBuilder.complianceStatus(tryGetField(rawPayload, "x-ms-compliance-status", String.class));
        tokenPayloadBuilder.vmDebuggable(tryGetField(rawPayload, "x-ms-sevsnpvm-is-debuggable", Boolean.class));
        tokenPayloadBuilder.ccePolicyDigest(tryGetField(rawPayload, "x-ms-sevsnpvm-hostdata", String.class));

        var  runtime = tryGetField(rawPayload, ("x-ms-runtime"), Map.class);

        if(runtime != null){
            var runtimeDataBuilder = RuntimeData.builder();
            runtimeDataBuilder.attestationUrl(tryGetField(runtime, "attestationUrl", String.class));
            runtimeDataBuilder.location(tryGetField(runtime, "location", String.class));
            runtimeDataBuilder.publicKey(tryGetField(runtime, "publicKey", String.class));
            tokenPayloadBuilder.runtimeData(runtimeDataBuilder.build());
        }

        return tokenPayloadBuilder.build();
    }
}
