package com.uid2.shared.secure;

public class AttestationClientException extends AttestationException
{
    public AttestationClientException(Throwable cause) {
        super(cause, true);
    }

    public AttestationClientException(String message) {
        super(message, true);
    }
}
