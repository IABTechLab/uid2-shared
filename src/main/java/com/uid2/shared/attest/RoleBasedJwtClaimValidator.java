package com.uid2.shared.attest;

import com.uid2.shared.auth.Role;

import java.util.Set;

public class RoleBasedJwtClaimValidator {
    private final Set<Role> requiredRoles;

    public RoleBasedJwtClaimValidator(Set<Role> requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    public Set<Role> getRequiredRoles() {
        return requiredRoles;
    }

    public boolean hasRequiredRoles(JwtValidationResponse response) {
        for (Role role : requiredRoles) {
            if (!response.hasRole(role)) {
                return false;
            }

        }

        return true;
    }
}
