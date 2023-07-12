package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;

public class TokenSignatureValidator implements ITokenSignatureValidator{
    @Override
    public TokenPayload validate(String tokenString) throws AttestationException {
        throw new UnsupportedOperationException();
    }
}
