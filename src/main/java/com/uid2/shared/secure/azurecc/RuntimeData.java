package com.uid2.shared.secure.azurecc;

import java.util.Base64;
import lombok.Builder;
import lombok.Value;

import java.nio.charset.StandardCharsets;

@Value
@Builder(toBuilder = true)
public class RuntimeData {
    private String attestationUrl;
    private String location;
    private String publicKey;

    public String getDecodedAttestationUrl() {
        if (attestationUrl != null) {
            return new String(Base64.getDecoder().decode(attestationUrl), StandardCharsets.UTF_8);
        }
        return null;
    }
}
