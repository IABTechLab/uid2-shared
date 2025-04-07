package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.util.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

public class ClientParser implements Parser<Collection<ClientKey>> {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Override
    public ParsingResult<Collection<ClientKey>> deserialize(InputStream inputStream) throws IOException {
        final ClientKey[] clientKeys = OBJECT_MAPPER.readValue(inputStream, ClientKey[].class);
        return new ParsingResult<>(Arrays.asList(clientKeys), clientKeys.length);
    }
}
