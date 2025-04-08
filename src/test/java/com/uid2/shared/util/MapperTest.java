package com.uid2.shared.util;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Test
    public void readValue_whenEnumIsCaseInsensitive() throws Exception {
        String json = """
                {
                    "omTestString": "test",
                    "omTestType": "tYpE_oNe"
                }
            """;
        OMTestObject omTestObject = OBJECT_MAPPER.readValue(json, OMTestObject.class);

        assertEquals(OMTestType.TYPE_ONE, omTestObject.omTestType());
    }

    @Test
    public void readValue_whenUnknownProperties() throws Exception {
        String json = """
                {
                    "omTestString": "test",
                    "omTestType": "TYPE_ONE",
                    "unknownType": "abcdef"
                }
            """;
        OMTestObject omTestObject = OBJECT_MAPPER.readValue(json, OMTestObject.class);
        OMTestObject expected = new OMTestObject("test", OMTestType.TYPE_ONE);

        assertEquals(expected, omTestObject);
    }

    @Test
    public void readValue_whenUnknownEnum() throws Exception {
        String json = """
                {
                    "omTestString": "test",
                    "omTestType": "TYPE_THREE"
                }
            """;
        OMTestObject omTestObject = OBJECT_MAPPER.readValue(json, OMTestObject.class);
        OMTestObject expected = new OMTestObject("test", OMTestType.UNKNOWN);

        assertEquals(expected, omTestObject);
    }

    @Test
    public void readValue_whenMissingProperties() throws Exception {
        String json = "{}";
        OMTestObject omTestObject = OBJECT_MAPPER.readValue(json, OMTestObject.class);
        OMTestObject expected = new OMTestObject(null, null);

        assertEquals(expected, omTestObject);
    }

    private enum OMTestType {
        TYPE_ONE,
        TYPE_TWO,
        @JsonEnumDefaultValue
        UNKNOWN
    }

    private record OMTestObject(String omTestString, OMTestType omTestType) {
    }
}
