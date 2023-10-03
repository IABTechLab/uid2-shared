package com.uid2.shared.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Test
    public void readValue_whenEnumIsCaseInsensitive() throws Exception {
        String json = "{\"omf_type\":\"tYpE_oNe\"}";
        OMFObject omfObject = OBJECT_MAPPER.readValue(json, OMFObject.class);

        assertEquals(OMFType.TYPE_ONE, omfObject.getOmfType());
    }

    @Test
    public void readValue_whenUnknownProperties() throws Exception {
        String json = "{\"omf_type\":\"TYPE_ONE\", \"unknown_type\":\"abcdef\"}";
        OMFObject omfObject = OBJECT_MAPPER.readValue(json, OMFObject.class);
        OMFObject expected = new OMFObject(OMFType.TYPE_ONE);

        assertEquals(expected, omfObject);
    }

    @Test
    public void readValue_whenMissingProperties() throws Exception {
        String json = "{}";
        OMFObject omfObject = OBJECT_MAPPER.readValue(json, OMFObject.class);
        OMFObject expected = new OMFObject(null);

        assertEquals(expected, omfObject);
    }

    private enum OMFType {
        TYPE_ONE,
        TYPE_TWO
    }

    private static class OMFObject {
        private final OMFType omfType;

        @JsonCreator
        public OMFObject(@JsonProperty("omf_type") OMFType omfType) {
            this.omfType = omfType;
        }

        public OMFType getOmfType() {
            return omfType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OMFObject omfObject = (OMFObject) o;
            return omfType == omfObject.omfType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(omfType);
        }
    }
}
