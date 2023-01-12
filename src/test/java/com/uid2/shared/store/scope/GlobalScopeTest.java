package com.uid2.shared.store.scope;

import com.uid2.shared.store.CloudPath;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalScopeTest {
    @Test
    void resolvesPathsRelativeToMetadataDirectory() {
        GlobalScope scope = new GlobalScope(new CloudPath("/a/b/c/meta.file"));
        CloudPath actual = scope.resolve(new CloudPath("d/file.json"));
        CloudPath expected = new CloudPath("/a/b/c/d/file.json");
        assertThat(actual).isEqualTo(expected);
    }
}