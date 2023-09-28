package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;

import java.security.PublicKey;

public interface IPublicKeyProvider {
    /**
     * Get Public Key from a MAA server.
     *
     * @param maaServerBaseUrl The Base Url of MAA server.
     * @param kid              The key id.
     * @return The public key.
     * @throws AttestationException
     */
    PublicKey GetPublicKey(String maaServerBaseUrl, String kid) throws AttestationException;
}
