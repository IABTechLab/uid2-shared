package com.uid2.shared.model;

import com.uid2.shared.model.KeyIdentifier;

public class EncryptedPayload {
    private final KeyIdentifier keyId;
    private final byte[] payload;

    public EncryptedPayload(KeyIdentifier keyId, byte[] payload) {
        this.keyId = keyId;
        this.payload = payload;
    }

    public KeyIdentifier getKeyId() {
        return keyId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
