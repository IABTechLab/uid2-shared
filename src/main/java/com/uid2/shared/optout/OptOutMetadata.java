package com.uid2.shared.optout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;

public class OptOutMetadata {
    private static ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("version")
    public long version;

    @JsonProperty("generated")
    public long generated;

    @JsonProperty("optout_logs")
    public Collection<OptOutFileMetadata> optoutLogs;

    public static OptOutMetadata fromJsonString(String str) {
        try {
            return OptOutMetadata.mapper.readValue(str, OptOutMetadata.class);
        } catch (JsonProcessingException ex) {
            // OptOutMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }

    public String toJsonString() {
        try {
            return OptOutMetadata.mapper.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            // OptOutMetadata is an internal message, any serialization and deserialization exception is logic error
            // return null here
            return null;
        }
    }
}
