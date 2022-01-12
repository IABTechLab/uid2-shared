package com.uid2.shared.auth;

public interface IAuthorizationProvider {
    boolean isAuthorized(IAuthorizable client);
}
