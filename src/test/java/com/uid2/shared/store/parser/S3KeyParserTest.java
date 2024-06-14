package com.uid2.shared.store.parser;

import com.uid2.shared.model.S3Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3KeyParserTest {

    private S3KeyParser parser;

    @BeforeEach
    void setUp() {
        parser = new S3KeyParser();
    }

    @Test
    void testDeserialize() throws IOException {
        String json = "[{" +
                "\"id\": 1, \"site_id\": 123, \"activates\": 1687635529, \"created\": 1687808329, \"secret\": \"S3keySecretByteHere1\"" +
                "},{" +
                "\"id\": 2, \"site_id\": 123, \"activates\": 1687808429, \"created\": 1687808329, \"secret\": \"S3keySecretByteHere2\"" +
                "},{" +
                "\"id\": 3, \"site_id\": 456, \"activates\": 1687635529, \"created\": 1687808329, \"secret\": \"S3keySecretByteHere3\"" +
                "}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        ParsingResult<Map<Integer, S3Key>> result = parser.deserialize(inputStream);

        assertNotNull(result);
        assertEquals(3, result.getData().size());
        assertTrue(result.getData().containsKey(1));
        assertTrue(result.getData().containsKey(2));
        assertTrue(result.getData().containsKey(3));
        assertEquals("S3keySecretByteHere1", result.getData().get(1).getSecret());
        assertEquals("S3keySecretByteHere2", result.getData().get(2).getSecret());
        assertEquals("S3keySecretByteHere3", result.getData().get(3).getSecret());
    }

    @Test
    void testDeserializeEmpty() throws IOException {
        String json = "[]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        ParsingResult<Map<Integer, S3Key>> result = parser.deserialize(inputStream);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testDeserializeInvalidJson() {
        String json = "[{\"id\": 1, \"site_id\": 123, \"activates\": 1687635529, \"created\": 1687808329, \"secret\": \"S3keySecretByteHere1\",]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        assertThrows(IOException.class, () -> parser.deserialize(inputStream));
    }
}
