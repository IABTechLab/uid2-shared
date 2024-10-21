package com.uid2.shared.secure;

public enum AttestationFailure {
    NONE,
    BAD_FORMAT,
    BAD_PAYLOAD,
    BAD_CERTIFICATE,
    FORBIDDEN_ENCLAVE,
    UNKNOWN_ATTESTATION_URL,
    INVALID_PROTOCOL,
    INTERNAL_ERROR,
    INVALID_TYPE,
    RESPONSE_ENCRYPTION_ERROR,
    UNKNOWN;

    public String explain() {
        switch (this) {
            case NONE:
                return "The operation succeeded";
            case BAD_FORMAT:
                return "The payload is ill-formatted";
            case BAD_PAYLOAD:
                return "Cannot verify payload with the signature";
            case BAD_CERTIFICATE:
                return "Cannot verify the certificate chain";
            case FORBIDDEN_ENCLAVE:
                return "The enclave identifier is unknown";
            case UNKNOWN_ATTESTATION_URL:
                return "The given attestation URL is unknown";
            case INVALID_PROTOCOL:
                return "The given protocol is not valid";
            case INTERNAL_ERROR:
                return "There was an internal processing error";
            case INVALID_TYPE:
                return "Invalid Operator Type";
            case RESPONSE_ENCRYPTION_ERROR:
                return "Error encrypting the response";
            default:
                return "Unknown reason";
        }
    }
}
