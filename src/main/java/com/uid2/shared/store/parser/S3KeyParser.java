package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.model.S3Key;
import com.uid2.shared.util.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class S3KeyParser implements Parser<Map<Integer, S3Key>> {
    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Override
    public ParsingResult<Map<Integer, S3Key>> deserialize(InputStream inputStream) throws IOException {
        S3Key[] s3Keys = OBJECT_MAPPER.readValue(inputStream, S3Key[].class);
        Map<Integer, S3Key> s3KeysMap = Arrays.stream(s3Keys)
                .collect(Collectors.toMap(S3Key::getId, s -> s));
        return new ParsingResult<>(s3KeysMap, s3KeysMap.size());
    }
}
