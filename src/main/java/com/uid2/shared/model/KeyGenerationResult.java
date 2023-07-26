package com.uid2.shared.model;

public class KeyGenerationResult {
    private final String key;
    private final String keyHash;

    public KeyGenerationResult(String key, String keyHash) {
        this.key = key;
        this.keyHash = keyHash;
    }

    public String getKey() {
        return key;
    }

    public String getKeyHash() {
        return keyHash;
    }
}
