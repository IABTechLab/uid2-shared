package com.uid2.shared.auth;

import java.util.Set;

public class RoleBasedAuthorizationProvider<E> implements IAuthorizationProvider {
    private final Set<E> allowedRoles;

    public RoleBasedAuthorizationProvider(Set<E> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    @Override
    public boolean isAuthorized(IAuthorizable client) {
        if (client == null || client.isDisabled()) {
            return false;
        }

        try {
            final IRoleAuthorizable<E> profile = (IRoleAuthorizable<E>) client;
            return this.allowedRoles.stream().anyMatch(profile::hasRole);
        } catch (Exception ex) {
            return false;
        }
    }
}
