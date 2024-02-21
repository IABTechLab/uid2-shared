package com.uid2.shared.secure;

public class AttestationClientException extends AttestationException
{
    private final AttestationFailure attestationFailure;

    public AttestationClientException(Throwable cause) {
        super(cause, true);
        this.attestationFailure = AttestationFailure.UNKNOWN;
    }

    public AttestationClientException(String message, AttestationFailure attestationFailure) {
        super(message, true);
        this.attestationFailure = attestationFailure;
    }

    public AttestationFailure getAttestationFailure() {
        return this.attestationFailure;
    }
}
