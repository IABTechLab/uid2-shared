package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SiteScopeTest {
    @Test
    void getMetadataPath() {
        SiteScope scope = new SiteScope(new CloudPath("/original/path/metadata.json"), 5);
        CloudPath expected = new CloudPath("/original/path/site/5/metadata.json");

        CloudPath actual = scope.getMetadataPath();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void resolvesPathsRelativeToSiteDirectory() {
        SiteScope scope = new SiteScope(new CloudPath("/original/path/metadata.json"), 5);
        CloudPath actual = scope.resolve(new CloudPath("file.xyz"));
        CloudPath expected = new CloudPath("/original/path/site/5/file.xyz");
        assertThat(actual).isEqualTo(expected);

    }
}