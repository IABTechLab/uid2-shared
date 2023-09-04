package com.uid2.shared.auth;

public interface IAuthorizableProvider {
    IAuthorizable get(String key);
}
