package com.uid2.shared.secure;

public class BadFormatException extends Exception {
    public BadFormatException(Throwable cause) {
        super(cause);
    }
    public BadFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
