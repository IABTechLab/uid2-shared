package com.uid2.shared.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.cloud.InMemoryStorageMock;
import com.uid2.shared.store.parser.Parser;
import com.uid2.shared.store.parser.ParsingResult;
import com.uid2.shared.store.scope.GlobalScope;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class ScopedStoreReaderTest {
    private final CloudPath metadataPath = new CloudPath("test/test-metadata.json");
    private final CloudPath dataPath = new CloudPath("test/data.json");
    private final String dataType = "test-data-type";
    private final GlobalScope scope = new GlobalScope(metadataPath);
    private InMemoryStorageMock storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageMock();
    }

    @Test
    void getMetadataPathReturnsPathFromScope() {
        ScopedStoreReader<TestData> reader = new ScopedStoreReader<>(storage, scope, new TestDataParser(), dataType);
        CloudPath actual = reader.getMetadataPath();
        assertThat(actual).isEqualTo(metadataPath);
    }

    @Test
    void raisesExceptionWhenLoadingContentWithNoMetadata() {
        ScopedStoreReader<TestData> reader = new ScopedStoreReader<>(storage, scope, new TestDataParser(), dataType);

        assertThatThrownBy(
                () -> reader.loadContent(null, dataType),
                "No metadata provided for loading data type test-data-type, can not load content"
        );
    }

    @Test
    void returnsNullIfMetadataFileMissing() throws Exception {
        ScopedStoreReader<TestData> reader = new ScopedStoreReader<>(storage, scope, new TestDataParser(), dataType);

        JsonObject metadata = reader.getMetadata();

        assertThat(metadata).isNull();
    }

    @Test
    void returnsMetadataWhenAvailable() throws Exception {
        JsonObject metadata = new JsonObject()
                .put(dataType, new JsonObject().put(
                        "location", dataPath.toString()
                ));

        storage.upload(toInputStream(metadata), metadataPath.toString());
        ScopedStoreReader<TestData> reader = new ScopedStoreReader<>(storage, scope, new TestDataParser(), dataType);

        JsonObject actual = reader.getMetadata();

        assertThat(actual).isEqualTo(metadata);
    }
    private static class TestData {
        private String field1;

        public TestData(String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }
    }

    private static class TestDataParser implements Parser<TestData> {
        ObjectMapper objectMapper = new ObjectMapper();
        @Override
        public ParsingResult<TestData> deserialize(InputStream inputStream) throws IOException {
            TestData data = objectMapper.readValue(inputStream, TestData.class);
            return new ParsingResult<>(data, 7);
        }
    }

    private static ByteArrayInputStream toInputStream(JsonObject metadata) {
        return new ByteArrayInputStream(metadata.encodePrettily().getBytes(StandardCharsets.UTF_8));
    }
}