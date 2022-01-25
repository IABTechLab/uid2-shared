// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.middleware;

import com.uid2.shared.attest.IAttestationTokenService;
import com.uid2.shared.auth.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class AttestationMiddleware {

    private final IAttestationTokenService tokenService;

    public AttestationMiddleware(IAttestationTokenService tokenService) {
        this.tokenService = tokenService;
    }

    //region RequestHandler

    public static final String AttestationTokenHeader = "Attestation-Token";

    public Handler<RoutingContext> handle(Handler<RoutingContext> handler) {
        final AttestationHandler wrapper = new AttestationHandler(handler, this.tokenService);
        return wrapper::handle;
    }

    private static class AttestationHandler {

        private final Handler<RoutingContext> next;
        private final IAttestationTokenService attestor;

        AttestationHandler(Handler<RoutingContext> next, IAttestationTokenService attestor) {
            this.next = next;
            this.attestor = attestor;
        }

        public void handle(RoutingContext rc) {
            boolean success = false;

            final IAuthorizable profile = AuthMiddleware.getAuthClient(rc);
            if (profile instanceof OperatorKey) {
                final String protocol = ((OperatorKey) profile).getProtocol();
                final String userToken = AuthMiddleware.getAuthToken(rc);
                final String encryptedToken = getAttestationToken(rc);
                if ("trusted".equals(protocol)) {
                    // (pre-)trusted operator requires no-attestation
                    success = true;
                } else if (encryptedToken != null && userToken != null) {
                    success = attestor.validateToken(userToken, encryptedToken);
                }
            }

            if (success) {
                next.handle(rc);
            } else {
                onFailedAttestation(rc);
            }
        }

        private void onFailedAttestation(RoutingContext rc) {
            rc.fail(401);
        }

        private String getAttestationToken(RoutingContext rc) {
            return rc.request().getHeader(AttestationTokenHeader);
        }
    }

    //endregion RequestHandler
}
