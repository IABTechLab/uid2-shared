package com.uid2.shared.secure;

public enum AttestationFailure {
    NONE,
    BAD_FORMAT,
    BAD_PAYLOAD,
    BAD_CERTIFICATE,
    FORBIDDEN_ENCLAVE,
    OTHER,
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
            case OTHER:
                return "Other reason, see related AttestationException for details";
            default:
                return "Unknown reason";
        }
    }
}
