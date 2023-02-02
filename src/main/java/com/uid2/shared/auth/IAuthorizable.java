package com.uid2.shared.auth;

public interface IAuthorizable {
    String getKey();
    String getContact();
    Integer getSiteId();
    boolean isDisabled();
}
