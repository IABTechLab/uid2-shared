package com.uid2.shared.secure;

public class AttestationException extends Exception {
    private final boolean isClientError;

    public boolean IsClientError() {
        return this.isClientError;
    }

    public AttestationException(Throwable cause, boolean isClientError) {
        super(cause);
        this.isClientError = isClientError;
    }

    public AttestationException(Throwable cause) {
        this(cause, false);
    }

    public AttestationException(String cause, boolean isClientError) {
        super(cause);
        this.isClientError = isClientError;
    }

    public AttestationException(String message) {
        this(message, false);
    }
}
