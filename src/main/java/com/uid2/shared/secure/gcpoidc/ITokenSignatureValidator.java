package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;

public interface ITokenSignatureValidator {
    /**
     * Validate token signature against authorized issuer.
     * @param tokenString
     * @return Parsed token payload.
     * @throws AttestationException
     */
    TokenPayload validate(String tokenString) throws AttestationException;
}
