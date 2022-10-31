package com.uid2.shared.auth;

public interface IRoleAuthorizable<E> extends IAuthorizable {
    boolean hasRole(E role);
}
