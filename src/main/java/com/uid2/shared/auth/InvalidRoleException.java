package com.uid2.shared.auth;

public class InvalidRoleException extends Exception {
    public InvalidRoleException(String msg) {
        super(msg);
    }

    public InvalidRoleException(String msg, Throwable t) {
        super(msg, t);
    }
}
