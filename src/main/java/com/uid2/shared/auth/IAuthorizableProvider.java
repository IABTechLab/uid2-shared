package com.uid2.shared.auth;

public interface IAuthorizableProvider {
    public IAuthorizable get(String key);
}
