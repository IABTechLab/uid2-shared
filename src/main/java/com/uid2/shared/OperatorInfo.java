package com.uid2.shared;

import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.auth.OperatorKey;
import io.vertx.ext.web.RoutingContext;

import static com.uid2.shared.middleware.AuthMiddleware.API_CLIENT_PROP;

/**
 * Given a logged in operator, determine its side id and if it's a public/private
 * Typically this should be extracting these details from the according OperatorKey
 */
public class OperatorInfo {
    public boolean isPublicOperator;
    public int siteId;

    public OperatorInfo(boolean isPublicOperator, int siteId) {
        this.isPublicOperator = isPublicOperator;
        this.siteId = siteId;
    }

    public static OperatorInfo getOperatorInfo(RoutingContext rc) throws Exception {
        IAuthorizable profile = (IAuthorizable)  rc.data().get(API_CLIENT_PROP);
        if (profile instanceof OperatorKey) {
            OperatorKey operatorKey = (OperatorKey) profile;
            return new OperatorInfo(operatorKey.isPublicOperator(), operatorKey.getSiteId());
        }
        throw new Exception("Cannot determine the operator type and site id from the profile");
    }
}


