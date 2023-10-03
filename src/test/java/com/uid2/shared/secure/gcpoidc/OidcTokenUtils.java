package com.uid2.shared.secure.gcpoidc;

import com.google.api.client.util.Clock;
import com.google.gson.JsonObject;
import com.uid2.shared.Const;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import static com.uid2.shared.secure.TestUtils.generateJwt;

public class OidcTokenUtils {
    public static TokenPayload validateAndParseToken(JsonObject payload, Clock clock) throws Exception{
        var gen = KeyPairGenerator.getInstance(Const.Name.AsymetricEncryptionKeyClass);
        gen.initialize(2048, new SecureRandom());
        var keyPair = gen.generateKeyPair();
        var privateKey = keyPair.getPrivate();
        var publicKey = keyPair.getPublic();

        // generate token
        var token = generateJwt(payload, privateKey);

        // init TokenSignatureValidator
        var tokenVerifier = new TokenSignatureValidator(publicKey, clock);

        // validate token
        return tokenVerifier.validate(token);
    }
}
