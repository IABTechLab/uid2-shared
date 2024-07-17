package com.uid2.shared.store.parser;

import com.uid2.shared.model.S3Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        ParsingResult<Map<Integer, S3Key>> result = parser.deserialize(inputStream);

        assertNotNull(result);
        assertEquals(3, result.getData().size());
        assertTrue(result.getData().containsKey(1));
        assertTrue(result.getData().containsKey(2));
        assertTrue(result.getData().containsKey(3));

        S3Key key1 = result.getData().get(1);
        assertEquals(1, key1.getId());
        assertEquals(123, key1.getSiteId());
        assertEquals(1687635529L, key1.getActivates());
        assertEquals(1687808329L, key1.getCreated());
        assertEquals("S3keySecretByteHere1", key1.getSecret());

        S3Key key2 = result.getData().get(2);
        assertEquals(2, key2.getId());
        assertEquals(123, key2.getSiteId());
        assertEquals(1687808429L, key2.getActivates());
        assertEquals(1687808329L, key2.getCreated());
        assertEquals("S3keySecretByteHere2", key2.getSecret());

        S3Key key3 = result.getData().get(3);
        assertEquals(3, key3.getId());
        assertEquals(456, key3.getSiteId());
        assertEquals(1687635529L, key3.getActivates());
        assertEquals(1687808329L, key3.getCreated());
        assertEquals("S3keySecretByteHere3", key3.getSecret());
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

    @Test
    void testS3KeySerialization() throws Exception {
        S3Key s3Key = new S3Key(1, 999, 1718689091L, 1718689091L, "64bNHMpU/mjaywjOpVacFOvEIFZmbYYUsNVNVu1jJZs=");
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(s3Key);

        String expectedJson = "{\"id\":1,\"siteId\":999,\"activates\":1718689091,\"created\":1718689091,\"secret\":\"64bNHMpU/mjaywjOpVacFOvEIFZmbYYUsNVNVu1jJZs=\"}";
        assertEquals(expectedJson, jsonString);
    }

    @Test
    void testDeserializeEndpointResults() throws IOException {
        String json = "[\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"siteId\": 999,\n" +
                "            \"activates\": 1720641670,\n" +
                "            \"created\": 1720641670,\n" +
                "            \"secret\": \"mydrCudb2PZOm01Qn0SpthltmexHUAA11Hy1m+uxjVw=\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 2,\n" +
                "            \"siteId\": 999,\n" +
                "            \"activates\": 1720728070,\n" +
                "            \"created\": 1720641670,\n" +
                "            \"secret\": \"FtdslrFSsvVXOuhOWGwEI+0QTkCvM8SGZAP3k2u3PgY=\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 3,\n" +
                "            \"siteId\": 999,\n" +
                "            \"activates\": 1720814470,\n" +
                "            \"created\": 1720641670,\n" +
                "            \"secret\": \"/7zO6QbKrhZKIV36G+cU9UR4hZUVg5bD+KjbczICjHw=\"\n" +
                "        }\n" +
                "    ]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        ParsingResult<Map<Integer, S3Key>> result = parser.deserialize(inputStream);

        assertNotNull(result);
        assertEquals(3, result.getCount());
        assertEquals(3, result.getData().size());
        assertTrue(result.getData().containsKey(1));
        assertTrue(result.getData().containsKey(2));
        assertTrue(result.getData().containsKey(3));

        S3Key key1 = result.getData().get(1);
        assertEquals(1, key1.getId());
        assertEquals(999, key1.getSiteId());
        assertEquals(1720641670L, key1.getActivates());
        assertEquals(1720641670L, key1.getCreated());
        assertEquals("mydrCudb2PZOm01Qn0SpthltmexHUAA11Hy1m+uxjVw=", key1.getSecret());

        S3Key key2 = result.getData().get(2);
        assertEquals(2, key2.getId());
        assertEquals(999, key2.getSiteId());
        assertEquals(1720728070L, key2.getActivates());
        assertEquals(1720641670L, key2.getCreated());
        assertEquals("FtdslrFSsvVXOuhOWGwEI+0QTkCvM8SGZAP3k2u3PgY=", key2.getSecret());

        S3Key key3 = result.getData().get(3);
        assertEquals(3, key3.getId());
        assertEquals(999, key3.getSiteId());
        assertEquals(1720814470L, key3.getActivates());
        assertEquals(1720641670L, key3.getCreated());
        assertEquals("/7zO6QbKrhZKIV36G+cU9UR4hZUVg5bD+KjbczICjHw=", key3.getSecret());
    }
}
