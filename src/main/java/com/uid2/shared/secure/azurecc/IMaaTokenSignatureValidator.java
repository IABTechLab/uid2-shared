package com.uid2.shared.secure.azurecc;

import com.uid2.shared.secure.AttestationException;
import com.uid2.shared.secure.Protocol;

public interface IMaaTokenSignatureValidator {
    /**
     * Validate token signature against authorized issuer.
     *
     * @param tokenString The raw MAA token string.
     * @return Parsed token payload.
     * @throws AttestationException
     */
    MaaTokenPayload validate(String tokenString, Protocol protocol) throws AttestationException;
}
