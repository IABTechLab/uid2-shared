package com.uid2.shared.secure.gcpoidc;

import com.uid2.shared.secure.AttestationException;

public class PolicyValidator implements IPolicyValidator{


    @Override
    public String getVersion() {
        return "V1";
    }

    @Override
    public String validate(TokenPayload payload) throws AttestationException {
        throw new UnsupportedOperationException();
    }
}
