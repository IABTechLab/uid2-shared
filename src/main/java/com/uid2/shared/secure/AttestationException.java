package com.uid2.shared.secure;

public class AttestationException extends Exception {
    public AttestationException(Throwable cause) {
        super(cause);
    }
    public AttestationException(String message) {
        super(message);
    }
}
