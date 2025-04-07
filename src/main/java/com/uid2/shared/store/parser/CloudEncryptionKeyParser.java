package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.model.CloudEncryptionKey;
import com.uid2.shared.util.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CloudEncryptionKeyParser implements Parser<Map<Integer, CloudEncryptionKey>> {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Override
    public ParsingResult<Map<Integer, CloudEncryptionKey>> deserialize(InputStream inputStream) throws IOException {
        final CloudEncryptionKey[] cloudEncryptionKeys = OBJECT_MAPPER.readValue(inputStream, CloudEncryptionKey[].class);
        final Map<Integer, CloudEncryptionKey> cloudEncryptionKeysMap = Arrays.stream(cloudEncryptionKeys)
                .collect(Collectors.toMap(CloudEncryptionKey::getId, s -> s));
        return new ParsingResult<>(cloudEncryptionKeysMap, cloudEncryptionKeysMap.size());
    }
}
