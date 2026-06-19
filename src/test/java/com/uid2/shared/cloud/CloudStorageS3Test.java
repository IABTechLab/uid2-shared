package com.uid2.shared.cloud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CloudStorageS3Test {

    @Test
    void constructorDoesNotNpeWhenCredentialEnvVarsAbsent() {
        // Old WebIdentityTokenFileCredentialsProvider called Paths.get(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE")),
        // which NPE'd when the env var was unset. DefaultCredentialsProvider must not throw at construction time.
        assertDoesNotThrow(() -> new CloudStorageS3("us-east-1", "test-bucket", "http://localhost:9999"));
    }
}
