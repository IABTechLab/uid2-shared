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

package com.uid2.shared.secure.gcp;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uid2.shared.secure.AttestationException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InstanceDocumentVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceDocumentVerifier.class);

    public static final boolean VERIFY_SIGNATURE = true;

    private GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
        .Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
        .build();

    public InstanceDocument verify(String token) throws Exception {
        GoogleIdToken googleId = GoogleIdToken.parse(verifier.getJsonFactory(), token);
        if (!VERIFY_SIGNATURE) {
            LOGGER.fatal("InstanceDocumentVerifier signature verification is ignored" );
        } else {
            if (!verifier.verify(googleId)) {
                throw new AttestationException("Unable to verify GCP VM's instance document");
            }
        }
        InstanceDocument id = new InstanceDocument(googleId);
        return id;
    }
}
