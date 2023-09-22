package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;

public interface IMaaTokenSignatureValidator {
    /**
     * Validate token signature against authorized issuer.
     * @param tokenString
     * @return Parsed token payload.
     * @throws AttestationException
     */
    MaaTokenPayload validate(String tokenString) throws AttestationException;
}
