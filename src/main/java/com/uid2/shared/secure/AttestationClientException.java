package com.uid2.shared.secure;

import lombok.Getter;

@Getter
public class AttestationClientException extends AttestationException {
    // This exception should be used when the error is as a result of invalid or bad data from the caller.
    // It will result in a return code in the 400s

    private final AttestationFailure attestationFailure;

    public AttestationClientException(Throwable cause) {
        super(cause, true);
        this.attestationFailure = AttestationFailure.UNKNOWN;
    }

    public AttestationClientException(String message, AttestationFailure attestationFailure) {
        super(message, true);
        this.attestationFailure = attestationFailure;
    }

}
