package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;

public interface IPolicyValidator {
    String getVersion();

    /**
     * Validate token payload against defined policies.
     * @param payload
     * @return Enclave ID.
     * @throws AttestationException
     */
    String validate(TokenPayload payload) throws AttestationException;
}
