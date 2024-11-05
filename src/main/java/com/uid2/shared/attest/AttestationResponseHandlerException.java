package com.uid2.shared.attest;

import lombok.Getter;

@Getter
public class AttestationResponseHandlerException extends Exception {
    private AttestationResponseCode responseCode;

    public AttestationResponseHandlerException(Throwable t) {
        super(t);
    }

    public AttestationResponseHandlerException(String message) {
        super(message);
    }

    public AttestationResponseHandlerException(AttestationResponseCode responseCode, String message) {
        super("AttestationResponseCode: " + String.valueOf(responseCode) + ", " + message);
        this.responseCode = responseCode;
    }

    public boolean isAttestationFailure() {
        return responseCode == AttestationResponseCode.AttestationFailure;
    }

}
