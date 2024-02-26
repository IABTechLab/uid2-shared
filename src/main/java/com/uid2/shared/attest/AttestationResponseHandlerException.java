package com.uid2.shared.attest;

public class AttestationResponseHandlerException extends Exception {
    private int statusCode = 0;

    public AttestationResponseHandlerException(Throwable t) {
        super(t);
    }

    public AttestationResponseHandlerException(String message) {
        super(message);
    }

    public AttestationResponseHandlerException(int statusCode, String message) {
        super("http status: " + String.valueOf(statusCode) + ", " + message);
        this.statusCode = statusCode;
    }

    public boolean isAttestationFailure() {
        return statusCode == 401;
    }
}
