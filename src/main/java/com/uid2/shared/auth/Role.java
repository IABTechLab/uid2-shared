package com.uid2.shared.auth;

public enum Role {
    GENERATOR,
    MAPPER,
    ID_READER,
    SHARER,
    OPERATOR,
    OPTOUT,
    CLIENTKEY_ISSUER,
    OPERATOR_MANAGER,
    SECRET_MANAGER,
    ADMINISTRATOR,
    //UID2-575 for cronjob to trigger private site data generated
    PRIVATE_SITE_REFRESHER
}
