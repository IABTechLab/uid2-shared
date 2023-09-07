package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.utils.ObjectMapperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientParser implements Parser<Map<String, ClientKey>> {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.build();

    @Override
    public ParsingResult<Map<String, ClientKey>> deserialize(InputStream inputStream) throws IOException {
        ClientKey[] clientKeys = OBJECT_MAPPER.readValue(inputStream, ClientKey[].class);
        Map<String, ClientKey> keyMap = Stream.of(clientKeys).collect(Collectors.toMap(
                ClientKey::getKey,
                k -> k
        ));
        return new ParsingResult<>(keyMap, clientKeys.length);
    }
}
