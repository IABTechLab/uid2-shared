package com.uid2.shared.secure;

public class AttestationException extends Exception {
    // Used to indicate an error in the processing of Attestation due to internal server errors
    // It will result in a response code of 500.
    // If the error is as a result in invalid input from the caller, use the AttestationClientException

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
