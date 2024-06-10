package com.uid2.shared.store;

import com.google.common.collect.ImmutableList;
import com.uid2.shared.cloud.InMemoryStorageMock;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.GlobalScope;
import static com.uid2.shared.TestUtilites.toInputStream;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ScopedStoreReaderTest {
    private final CloudPath metadataPath = new CloudPath("test/test-metadata.json");
    private final CloudPath dataPath = new CloudPath("test/data.json");
    private final String dataType = "test-data-type";
    private final GlobalScope scope = new GlobalScope(metadataPath);
    private final JsonObject metadata =  new JsonObject()
            .put(dataType, new JsonObject().put(
                    "location", dataPath.toString()
            ));
    private InMemoryStorageMock storage;
    private final TestDataParser parser = new TestDataParser();

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageMock();
    }

    @Test
    void getMetadataPathReturnsPathFromScope() {
        ScopedStoreReader<Collection<TestData>> reader = new ScopedStoreReader<>(storage, scope, parser, dataType);
        CloudPath actual = reader.getMetadataPath();
        assertThat(actual).isEqualTo(metadataPath);
    }

    @Test
    void raisesExceptionWhenLoadingContentWithNoMetadata() {
        ScopedStoreReader<Collection<TestData>> reader = new ScopedStoreReader<>(storage, scope, parser, dataType);

        assertThatThrownBy(
                () -> reader.loadContent(null, dataType),
                "No metadata provided for loading data type test-data-type, can not load content"
        );
    }

    @Test
    void returnsMetadataWhenAvailable() throws Exception {
        storage.upload(toInputStream(metadata.encodePrettily()), metadataPath.toString());
        ScopedStoreReader<Collection<TestData>> reader = new ScopedStoreReader<>(storage, scope, parser, dataType);

        JsonObject actual = reader.getMetadata();

        assertThat(actual).isEqualTo(metadata);
    }

    @Test
    void returnsLoadedSnapshot() throws Exception {
        storage.upload(toInputStream("value1,value2"), dataPath.toString());
        ScopedStoreReader<Collection<TestData>> reader = new ScopedStoreReader<>(storage, scope, parser, dataType);

        reader.loadContent(metadata, dataType);
        Collection<TestData> actual = reader.getSnapshot();
        Collection<TestData> expected = ImmutableList.of(
                new TestData("value1"),
                new TestData("value2")
        );
        assertThat(actual).isEqualTo(expected);
    }

    private static class TestData {
        private final String field1;

        public TestData(String field1) {
            this.field1 = field1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return Objects.equals(field1, testData.field1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1);
        }
    }

    private static class TestDataParser implements Parser<Collection<TestData>> {
        @Override
        public ParsingResult<Collection<TestData>> deserialize(InputStream inputStream) throws IOException {
            List<TestData> result = Arrays.stream(readInputStream(inputStream)
                    .split(","))
                    .map(TestData::new)
                    .collect(Collectors.toList());
            return new ParsingResult<>(result, result.size());
        }

        private static String readInputStream(InputStream inputStream) {
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));
        }
    }
}