package com.uid2.shared;

import com.uid2.shared.auth.IAuthorizable;
import com.uid2.shared.auth.OperatorKey;
import io.vertx.ext.web.RoutingContext;

import static com.uid2.shared.middleware.AuthMiddleware.API_CLIENT_PROP;

public class OperatorInfo {
    public boolean isPublicOperator;
    public int siteId;

    public OperatorInfo(boolean isPublicOperator, int siteId) {
        this.isPublicOperator = isPublicOperator;
        this.siteId = siteId;
    }

    public static OperatorInfo getOperatorInfo(RoutingContext rc) {
        IAuthorizable profile = (IAuthorizable)  rc.data().get(API_CLIENT_PROP);
        if (profile instanceof OperatorKey) {
            OperatorKey operatorKey = (OperatorKey) profile;
            return new OperatorInfo(operatorKey.isPublicOperator(), operatorKey.getSiteId());
        }
        return new OperatorInfo(true, 0);
    }
}


