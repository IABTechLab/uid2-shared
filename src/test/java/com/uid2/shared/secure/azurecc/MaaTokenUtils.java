package com.uid2.shared.secure.azurecc;

import com.google.api.client.util.Clock;
import com.google.gson.JsonObject;
import com.uid2.shared.Const;
import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.Protocol;

import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import static com.uid2.shared.secure.TestUtils.generateJwt;

public class MaaTokenUtils {
    public static final String MAA_BASE_URL = "https://sharedeus.eus.attest.azure.net";

    public static MaaTokenPayload validateAndParseToken(JsonObject payload, Clock clock, Protocol protocol) throws Exception{
        var gen = KeyPairGenerator.getInstance(Const.Name.AsymetricEncryptionKeyClass);
        gen.initialize(2048, new SecureRandom());
        var keyPair = gen.generateKeyPair();
        var privateKey = keyPair.getPrivate();
        var publicKey = keyPair.getPublic();

        // generate token
        var token = generateJwt(payload, privateKey);

        var keyProvider = new MockKeyProvider(publicKey);

        // init TokenSignatureValidator
        var tokenVerifier = new MaaTokenSignatureValidator(MAA_BASE_URL, keyProvider, clock);

        // validate token
        return tokenVerifier.validate(token, protocol);
    }

    private static class MockKeyProvider implements IPublicKeyProvider {

        private final PublicKey publicKey;

        MockKeyProvider(PublicKey publicKey){
            this.publicKey = publicKey;
        }

        @Override
        public PublicKey GetPublicKey(String maaServerBaseUrl, String kid) throws AttestationException {
            return this.publicKey;
        }
    }
}
