package com.uid2.shared.auth;

public enum Role {
    GENERATOR,
    MAPPER,
    ID_READER,
    SHARER,
    OPERATOR,
    OPTOUT, // Used for operators to call optout service's /optout/refresh API
    OPTOUT_SERVICE, // UID2-912 - Specifically for optout service only
    CLIENTKEY_ISSUER,
    OPERATOR_MANAGER,
    SECRET_MANAGER,
    ADMINISTRATOR,
    SHARING_PORTAL,
    PRIVATE_SITE_REFRESHER // UID2-575 - For cronjob to trigger private site data generated
}
