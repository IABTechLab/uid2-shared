package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;

public interface IPolicyValidator {
    /**
     * Validate token payload against defined policies.
     * @param maaTokenPayload the parsed MAA token.
     * @param publicKey the public key info to verify in payload runtime data
     * @return
     * @throws AttestationException
     */
    String validate(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException;
}
