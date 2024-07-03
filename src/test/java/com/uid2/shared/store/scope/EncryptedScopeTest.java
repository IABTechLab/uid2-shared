package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;


class EncryptedScopeTest {
    @Test
    void getMetadataPath() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5);
        CloudPath expected = new CloudPath("/original/path/encryption/site/5/metadata.json");

        CloudPath actual = scope.getMetadataPath();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void resolvesPathsRelativeToSiteDirectory() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5);
        CloudPath actual = scope.resolve(new CloudPath("file.xyz"));
        CloudPath expected = new CloudPath("/original/path/encryption/site/5/file.xyz");
        assertThat(actual).isEqualTo(expected);

    }
}