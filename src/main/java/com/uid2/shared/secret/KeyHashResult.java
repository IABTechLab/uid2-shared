package com.uid2.shared.secret;

public class KeyHashResult {
    public final String hash;
    public final String salt;

    public KeyHashResult(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
    }
}
