package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.utils.ObjectMapperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClientParser implements Parser<Collection<ClientKey>> {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.build();

    @Override
    public ParsingResult<Collection<ClientKey>> deserialize(InputStream inputStream) throws IOException {
        ClientKey[] clientKeys = OBJECT_MAPPER.readValue(inputStream, ClientKey[].class);
        return new ParsingResult<>(Arrays.asList(clientKeys), clientKeys.length);
    }
}
