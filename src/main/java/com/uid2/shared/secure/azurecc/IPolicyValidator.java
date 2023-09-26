package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;

public interface IPolicyValidator {
    /**
     * Validate token payload against defined policies.
     *
     * @param maaTokenPayload The parsed MAA token.
     * @param publicKey       The public key info to verify in payload runtime data.
     * @return The enclave id.
     * @throws AttestationException
     */
    String validate(MaaTokenPayload maaTokenPayload, String publicKey) throws AttestationException;
}
