package com.uid2.shared.secure;

import java.util.Base64;
import java.util.Objects;

public class NitroEnclaveIdentifier {

    private String base64String;

    private NitroEnclaveIdentifier(String base64String) {
        this.base64String = base64String;
    }

    public static NitroEnclaveIdentifier fromBase64(String base64String) {
        return new NitroEnclaveIdentifier(base64String);
    }

    public static NitroEnclaveIdentifier fromRaw(byte[] bytes) {
        return new NitroEnclaveIdentifier(Base64.getEncoder().encodeToString(bytes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NitroEnclaveIdentifier that = (NitroEnclaveIdentifier) o;
        return Objects.equals(base64String, that.base64String);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base64String);
    }

    @Override
    public String toString() {
        return "nitro: " + base64String;
    }
}
