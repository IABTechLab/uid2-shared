package com.uid2.shared.auth;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Role {
    GENERATOR,
    MAPPER,
    ID_READER,
    SHARER,
    OPERATOR,
    OPTOUT, // Used for operators to call optout service's /optout/refresh API
    OPTOUT_SERVICE, // UID2-912 - Specifically for optout service only
    @Deprecated
    CLIENTKEY_ISSUER,
    @Deprecated
    OPERATOR_MANAGER,
    @Deprecated
    SECRET_MANAGER,
    @Deprecated
    ADMINISTRATOR,
    SHARING_PORTAL, // corresponds to custom okta scope 'uid2.admin.ss-portal'
    PRIVATE_SITE_REFRESHER, // UID2-575 - For cronjob to trigger private site data generated
    ALL, // corresponds to 'developer' okta group
    PRIVILEGED, // corresponds to 'developer-elevated' okta group
    SUPER_USER, // corresponds to 'admin' okta group
    SECRET_ROTATION, // corresponds to custom okta scope 'uid2.admin.secret-rotation'
    PRIVATE_OPERATOR_SYNC, // corresponds to custom okta scope 'uid2.admin.site-sync'
    @JsonEnumDefaultValue
    UNKNOWN
}
