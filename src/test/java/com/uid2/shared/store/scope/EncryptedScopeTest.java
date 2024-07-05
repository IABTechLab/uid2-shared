package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EncryptedScopeTest {
    @Test
    void getMetadataPath() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5,false);
        CloudPath expected = new CloudPath("/original/path/encrypted/5_private/metadata.json");

        CloudPath actual = scope.getMetadataPath();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void resolvesPathsRelativeToSiteDirectory() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5,false );
        CloudPath actual = scope.resolve(new CloudPath("file.xyz"));
        CloudPath expected = new CloudPath("/original/path/encrypted/5_private/file.xyz");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getMetadataPathPublic() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5, true);
        CloudPath expected = new CloudPath("/original/path/encrypted/5_public/metadata.json");

        CloudPath actual = scope.getMetadataPath();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void resolvesPathsRelativeToSiteDirectoryPublic() {
        EncryptedScope scope = new EncryptedScope(new CloudPath("/original/path/metadata.json"), 5, true);
        CloudPath actual = scope.resolve(new CloudPath("file.xyz"));
        CloudPath expected = new CloudPath("/original/path/encrypted/5_public/file.xyz");
        assertThat(actual).isEqualTo(expected);
    }
}
