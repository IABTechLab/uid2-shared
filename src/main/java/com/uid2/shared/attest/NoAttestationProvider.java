package com.uid2.shared.attest;

import com.uid2.enclave.IAttestationProvider;

public class NoAttestationProvider implements IAttestationProvider {
    @Override
    public byte[] getAttestationRequest(byte[] publicKey) {
        byte[] req = {0};
        return req;
    }
}
