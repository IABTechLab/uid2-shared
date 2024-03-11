package com.uid2.shared.secure.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import com.uid2.shared.Const;
import com.uid2.shared.cloud.CloudUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class InstanceDocumentVerifierTest {

    private static final String GOOGLE_CREDENTIALS = "<placeholder>";
    private static final String INSTANCE_DOCUMENT = "<placeholder>";

    public static InstanceDocument getTestInstanceDocument() throws Exception {
        InstanceDocumentVerifier verifier = new InstanceDocumentVerifier();
        return verifier.verify(INSTANCE_DOCUMENT);
    }

    @Test
    public void verifyToken() throws Exception {
        assumeTrue(INSTANCE_DOCUMENT.length() > 20);

        InstanceDocument id = getTestInstanceDocument();
    }

    @Test
    public void loadEncodedCredentials() {
        assumeTrue(GOOGLE_CREDENTIALS.length() > 20);

        JsonObject config = new JsonObject();
        config.put(Const.Config.GoogleCredentialsProp, GOOGLE_CREDENTIALS);

        GoogleCredentials credentials = CloudUtils.getGoogleCredentialsFromConfig(config);
    }
}
