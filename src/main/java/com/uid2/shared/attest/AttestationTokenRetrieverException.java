package com.uid2.shared.attest;

public class AttestationTokenRetrieverException extends Exception {
    private int statusCode = 0;

    public AttestationTokenRetrieverException(Throwable t) {
        super(t);
    }

    public AttestationTokenRetrieverException(String message) {
        super(message);
    }

    public AttestationTokenRetrieverException(int statusCode, String message) {
        super("http status: " + String.valueOf(statusCode) + ", " + message);
        this.statusCode = statusCode;
    }

    public boolean isAttestationFailure() {
        return statusCode == 401;
    }
}
