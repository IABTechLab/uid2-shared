package com.uid2.shared.optout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OptOutFileMetadata {
    private static ObjectMapper mapper = new ObjectMapper();

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
            return OptOutFileMetadata.mapper.readValue(str, OptOutFileMetadata.class);
        } catch (JsonProcessingException ex) {
            // OptOutFileMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }

    public String toJsonString() {
        try {
            return OptOutFileMetadata.mapper.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            // OptOutFileMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }

}
