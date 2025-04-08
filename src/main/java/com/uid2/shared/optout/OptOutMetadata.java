package com.uid2.shared.optout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.util.Mapper;

import java.util.Collection;

public class OptOutMetadata {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @JsonProperty("version")
    public long version;

    @JsonProperty("generated")
    public long generated;

    @JsonProperty("optout_logs")
    public Collection<OptOutFileMetadata> optoutLogs;

    public static OptOutMetadata fromJsonString(String str) {
        try {
            return OptOutMetadata.OBJECT_MAPPER.readValue(str, OptOutMetadata.class);
        } catch (JsonProcessingException ex) {
            // OptOutMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }

    public String toJsonString() {
        try {
            return OptOutMetadata.OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            // OptOutMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }
}
