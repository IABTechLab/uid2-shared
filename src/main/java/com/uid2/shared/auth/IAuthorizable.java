package com.uid2.shared.auth;

public interface IAuthorizable {
    String getKeyHash();
    String getKeySalt();
    String getContact();
    Integer getSiteId();
    boolean isDisabled();
    String getKeyId();
}
