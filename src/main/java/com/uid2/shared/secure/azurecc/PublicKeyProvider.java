package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;

import java.security.PublicKey;

public interface PublicKeyProvider {
    PublicKey GetPublicKey(String maaServerBaseUrl, String kid) throws AttestationException;
}
