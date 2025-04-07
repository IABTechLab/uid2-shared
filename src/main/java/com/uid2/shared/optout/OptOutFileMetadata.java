package com.uid2.shared.optout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.util.Mapper;

public class OptOutFileMetadata {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @JsonProperty("type")
    public String type;

    @JsonProperty("from")
    public long from;

    @JsonProperty("to")
    public long to;

    @JsonProperty("location")
    public String location;

    public static OptOutFileMetadata fromJsonString(String str) {
        try {
            return OptOutFileMetadata.OBJECT_MAPPER.readValue(str, OptOutFileMetadata.class);
        } catch (JsonProcessingException ex) {
            // OptOutFileMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }

    public String toJsonString() {
        try {
            return OptOutFileMetadata.OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            // OptOutFileMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }
}
