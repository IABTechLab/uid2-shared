package com.uid2.shared.attest;

public class UidCoreClientException extends Exception {

    private int statusCode = 0;

    public UidCoreClientException(Throwable t) {
        super(t);
    }

    public UidCoreClientException(String message) {
        super(message);
    }

    public UidCoreClientException(int statusCode, String message) {
        super("http status: " + String.valueOf(statusCode) + ", " + message);
        this.statusCode = statusCode;
    }

    public boolean couldBeAttestationFailure() {
        return statusCode == 401;
    }
}
