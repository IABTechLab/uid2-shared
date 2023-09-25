package com.uid2.shared.secure;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Clock;
import com.google.api.client.util.SecurityUtils;
import com.google.api.client.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uid2.shared.Const;
import com.uid2.shared.secure.gcpoidc.TokenPayload;
import com.uid2.shared.secure.gcpoidc.TokenSignatureValidator;

import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;

public class TestUtils {
    public static String generateJwt(JsonObject payload, PrivateKey privateKey) throws Exception {
        var jsonFactory = new GsonFactory();
        var header = new JsonWebSignature.Header();
        header.setAlgorithm("RS256");
        header.setType("JWT");
        header.setKeyId("dummy");
        String content = Base64.encodeBase64URLSafeString(jsonFactory.toByteArray(header)) + "." + Base64.encodeBase64URLSafeString(payload.toString().getBytes());
        byte[] contentBytes = StringUtils.getBytesUtf8(content);
        byte[] signature = SecurityUtils.sign(SecurityUtils.getSha256WithRsaSignatureAlgorithm(), privateKey, contentBytes);
        return content + "." + Base64.encodeBase64URLSafeString(signature);
    }

    public static JsonObject loadFromJson(String fileName) throws IOException {
        var jsonStr = new String(TestUtils.class.getResourceAsStream(fileName).readAllBytes());
        var payload = new Gson().fromJson(jsonStr, JsonObject.class);
        return payload;
    }
}
