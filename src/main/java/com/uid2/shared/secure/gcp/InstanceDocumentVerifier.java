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
