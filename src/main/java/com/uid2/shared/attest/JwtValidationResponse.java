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
    private Exception validationException;

    public JwtValidationResponse(boolean isValid) {
        this.isValid = isValid;
    }

    public JwtValidationResponse withRoles(Role... roles) {
        this.roles = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles)));
        return this;
    }
    public JwtValidationResponse withRoles(String roles) {
        String[] parts = roles.split(",");
        var t = Arrays.stream(parts).map(r -> Role.valueOf(r)).collect(Collectors.toList());
        this.roles = Set.copyOf(t);
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

    public Exception getValidationException() {
        return validationException;
    }

    public void setValidationException(Exception validationException) {
        this.validationException = validationException;
    }
}
