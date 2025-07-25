package com.uid2.shared.attest;

import com.uid2.shared.auth.Role;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtValidationResponse {
    private boolean isValid;
    private Set<Role> roles;
    private Integer siteId;
    private String enclaveId;
    private String enclaveType;
    private String operatorVersion;

    private String audience;
    private String subject;
    private String jti;

    public JwtValidationResponse(boolean isValid) {
        this.isValid = isValid;
    }

    public JwtValidationResponse withRoles(Role... roles) {
        this.roles = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles)));
        return this;
    }
    public JwtValidationResponse withRoles(String roles) {
        if (roles != null && !roles.isBlank()) {
            String[] parts = roles.split(",");
            var t = Arrays.stream(parts).map(r -> Role.valueOf(r)).collect(Collectors.toList());
            this.roles = Set.copyOf(t);
        } else {
            this.roles = Set.of();
        }
        return this;
    }

    public JwtValidationResponse withSiteId(Integer siteId) {
        this.siteId = siteId;
        return this;
    }

    public JwtValidationResponse withEnclaveId(String enclaveId) {
        this.enclaveId =enclaveId;
        return this;
    }
    public JwtValidationResponse withEnclaveType(String enclaveType) {
        this.enclaveType =enclaveType;
        return this;
    }

    public JwtValidationResponse withOperatorVersion(String operatorVersion) {
        this.operatorVersion = operatorVersion;
        return this;
    }

    public JwtValidationResponse withAudience(String audience) {
        this.audience = audience;
        return this;
    }
    public JwtValidationResponse withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public JwtValidationResponse withJti(String jti) {
        this.jti = jti;
        return this;
    }

    public Set<Role> getRoles() {
        return this.roles;
    }
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public boolean getIsValid() {
        return this.isValid;
    }
    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public Integer getSiteId() {
        return this.siteId;
    }

    public String getEnclaveId() {
        return this.enclaveId;
    }

    public String getEnclaveType() {
        return enclaveType;
    }

    public String getOperatorVersion() {
        return operatorVersion;
    }

    public String getAudience() {
        return audience;
    }

    public String getSubject() {
        return subject;
    }

    public String getJti() { return jti; }
}
