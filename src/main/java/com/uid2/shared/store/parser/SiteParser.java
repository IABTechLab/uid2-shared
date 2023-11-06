package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uid2.shared.model.Site;
import com.uid2.shared.util.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SiteParser implements Parser<Map<Integer,Site>> {

    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    @Override
    public ParsingResult<Map<Integer, Site>> deserialize(InputStream inputStream) throws IOException {
        Site[] sites = OBJECT_MAPPER.readValue(inputStream, Site[].class);
        Map<Integer, Site> sitesMap = Arrays.stream(sites)
                .collect(Collectors.toMap(Site::getId, s -> s));
        return new ParsingResult<>(sitesMap, sitesMap.size());
    }
}
