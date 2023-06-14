package com.uid2.shared.auth;

public class InvalidRoleException extends Exception {
    public InvalidRoleException(Throwable t) {
        super(t);
    }
    public InvalidRoleException(String msg, Throwable t) {
        super(msg, t);
    }
}
