package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.util.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ClientParserTest {

    private ClientParser parser;

    @BeforeEach
    void setUp() {
        parser = new ClientParser();
    }

    @Test
    void testDeserialize() throws IOException {
        String json = "[\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-999-fCXrMM.fsR3mDqAXELtWWMS+xG1s7RdgRTMqdOH2qaAo=\",\n" +
                "    \"secret\": \"DzBzbjTJcYL0swDtFs2krRNu+g1Eokm2tBU4dEuD0Wk=\",\n" +
                "    \"name\": \"Special\",\n" +
                "    \"contact\": \"Special\",\n" +
                "    \"created\": 1701210253,\n" +
                "    \"roles\": [\n" +
                "      \"MAPPER\",\n" +
                "      \"GENERATOR\",\n" +
                "      \"ID_READER\",\n" +
                "      \"SHARER\",\n" +
                "      \"OPTOUT\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 999,\n" +
                "    \"key_hash\": \"fsSGnDxa/V9eJZ9Tas+dowwyO/X1UsC68RN9qM2xUu9ZOaKEOv9EVd7pkt3As/nE5B6TRu0PzK+IDzSQhD1+rw==\",\n" +
                "    \"key_salt\": \"jySwjjqo9O6OU01OWujBWC6xZNpBqRTk5H7K2borcFA=\",\n" +
                "    \"key_id\": \"UID2-C-L-999-fCXrM\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"LOCALbGlvbnVuZGVybGluZXdpbmRzY2FyZWRzb2Z0ZGVzZXI=\",\n" +
                "    \"secret\": \"c3RlZXBzcGVuZHNsb3BlZnJlcXVlbnRseWRvd2lkZWM=\",\n" +
                "    \"name\": \"Special (Legacy Key)\",\n" +
                "    \"contact\": \"Special (Legacy Key)\",\n" +
                "    \"created\": 1701210253,\n" +
                "    \"roles\": [\n" +
                "      \"MAPPER\",\n" +
                "      \"GENERATOR\",\n" +
                "      \"ID_READER\",\n" +
                "      \"SHARER\",\n" +
                "      \"OPTOUT\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 999,\n" +
                "    \"key_hash\": \"OPIi+MWKNz41wzu+atsBLAtXDTLFhLWPq5mCxA3L8anX+fjKaVDAf55D98BSLAh/EFQE/xTQyo/YK5snPS8ivA==\",\n" +
                "    \"key_salt\": \"FpgbvHGqpVhi3I/b8/9HnguiycUzb2y+KsdicPpNLJI=\",\n" +
                "    \"key_id\": \"LOCALbGlvb\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-123-t32pCM.5NCX1E94UgOd2f8zhsKmxzCoyhXohHYSSWR8U=\",\n" +
                "    \"secret\": \"FsD4bvtjMkeTonx6HvQp6u0EiI1ApGH4pIZzZ5P7UcQ=\",\n" +
                "    \"name\": \"DSP\",\n" +
                "    \"contact\": \"DSP\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"ID_READER\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 123,\n" +
                "    \"key_hash\": \"vVb/MjymmYAE3L6as5t1DCjbts4bT2wZh4V4iAagOAe97jthFmT4YAb6gGVfEs4Pq+CqNPgz+X338RNRa8NOlA==\",\n" +
                "    \"key_salt\": \"G36g+KxlS+z5NwSXUOnBtc9yJKHECvXgjbS13X5A7rw=\",\n" +
                "    \"key_id\": \"UID2-C-L-123-t32pC\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-124-H8VwqX.l2G4TCuUWYAqdqkeG/UqtFoPEoXirKn4kHWxc=\",\n" +
                "    \"secret\": \"NcMgi6Y8C80SlxvV7pYlfcvEIo+2b0508tYQ3pKK8HM=\",\n" +
                "    \"name\": \"Publisher\",\n" +
                "    \"contact\": \"Publisher\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"GENERATOR\",\n" +
                "      \"ID_READER\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 124,\n" +
                "    \"key_hash\": \"uA1aRDR9owk53W47zZpI6cS/bRVgKm4ggRvr9m0pz+I/5IzQcIQqfurm1Ors96r8Q2xC8GZVG3spwR/H89rQmA==\",\n" +
                "    \"key_salt\": \"rSwnZ5aKauMLPLMHvvH25C1LU5MdJv5+fjQ5/Yy5hP0=\",\n" +
                "    \"key_id\": \"UID2-C-L-124-H8Vwq\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-125-E5w9L8.T5og45yFqQeoj4ubh9IVqXcaSVwk7A5XyG958=\",\n" +
                "    \"secret\": \"3YAgjckHGQyBgSFj64ZsLf8WlUnvrQhLKuG7rljp6W4=\",\n" +
                "    \"name\": \"Data Provider\",\n" +
                "    \"contact\": \"Data Provider\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"MAPPER\",\n" +
                "      \"SHARER\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 125,\n" +
                "    \"key_hash\": \"0GFyfie9Vz7INDxG5gT3MeHOnrXdIy/H9I5OrTZHx/cX5zToF8BngbseREbeEG7xH3KFs5TfSdwI5N/OWzYrGQ==\",\n" +
                "    \"key_salt\": \"taG4CBJ1F4aWwL8XwyilKl9WzYSWoG9RjvB4BGqf0/w=\",\n" +
                "    \"key_id\": \"UID2-C-L-125-E5w9L\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-126-GMP9tD.jn2o3vmXzn7vmKRlTT6BEUrPUaPJuQmDBdq38=\",\n" +
                "    \"secret\": \"1ydXM0rEj+ROUazVpZjNZOGu2T5+f/BIiBfnK8xGh/A=\",\n" +
                "    \"name\": \"Advertiser\",\n" +
                "    \"contact\": \"Advertiser\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"MAPPER\",\n" +
                "      \"SHARER\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 126,\n" +
                "    \"key_hash\": \"lDl6HiO7hVdXmHm+gogCZmiCzhWcDLVIxBItR+0GMBWpRxleIr2HQG2oAHVKYd63AKeMZGwh5svbbJ6Gu0RUMQ==\",\n" +
                "    \"key_salt\": \"FH6UNMUCJKday6FWTLUtmg9Hwh4Rd/HhenfjtRyaAEI=\",\n" +
                "    \"key_id\": \"UID2-C-L-126-GMP9t\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-127-aHVydH.JlZnVzZWRmYXN0ZW5lbXliYWdpZGVudGl0eWU=\",\n" +
                "    \"secret\": \"c3VpdG9wcG9zaXRlaW1hZ2Vsb29rc2ltcGxlc3RmaXI=\",\n" +
                "    \"name\": \"OptOut\",\n" +
                "    \"contact\": \"OptOut\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"OPTOUT\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 127,\n" +
                "    \"key_hash\": \"BEEnHVPHwbMYUtZk/N6jjnN04U7xpu6hV5yF4Nn2Zw9pigD43JLZdEleRW/Mz7LAQfYtLTJk768J8WK6F4Ku/Q==\",\n" +
                "    \"key_salt\": \"cw9wfsevy1xRiPys5JkBSTPHmTickWkFWQ0zrIF2C60=\",\n" +
                "    \"key_id\": \"UID2-C-L-127-aHVyd\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"UID2-C-L-1000-qxpBsF.ibeCDBpD2bq4Zm7inDacGioUk1aaLeNJrabow=\",\n" +
                "    \"secret\": \"VT7+t0G/RVueMuVZAL56I2c3JJFSYQfhbu8yo0V/Tds=\",\n" +
                "    \"name\": \"Legacy Site Client\",\n" +
                "    \"contact\": \"Legacy Site Client\",\n" +
                "    \"created\": 1609459200,\n" +
                "    \"roles\": [\n" +
                "      \"MAPPER\",\n" +
                "      \"GENERATOR\",\n" +
                "      \"ID_READER\",\n" +
                "      \"SHARER\",\n" +
                "      \"OPTOUT\"\n" +
                "    ],\n" +
                "    \"disabled\": false,\n" +
                "    \"site_id\": 1000,\n" +
                "    \"key_hash\": \"654FIeR8DFtLi5AC8RXvwfBQ1b9J8L+dVyJUxoTSCpMBQ3z937CxQ1fp40fHIs9SbQPnivBMV5s+TdDMZXZqgQ==\",\n" +
                "    \"key_salt\": \"huTnT+HyINotMK0W00Gy7VGaQT9XR0KaxZTBvVuTCF0=\",\n" +
                "    \"key_id\": \"UID2-C-L-1000-qxpBs\"\n" +
                "  }\n" +
                "]";
        ObjectMapper OBJECT_MAPPER = new ObjectMapper()
//                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);//Mapper.getInstance();
        ClientKey[] clientKeys = OBJECT_MAPPER.readValue(json, ClientKey[].class);

        assertNotNull(clientKeys);
    }
}
