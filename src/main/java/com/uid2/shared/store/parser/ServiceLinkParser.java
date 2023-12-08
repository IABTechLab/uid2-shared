package com.uid2.shared.store.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.model.ServiceLink;
import com.uid2.shared.util.Mapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceLinkParser implements Parser<Map<String, ServiceLink>> {

    private static final ObjectMapper OBJECT_MAPPER = Mapper.getInstance();

    static {
        OBJECT_MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    @Override
    public ParsingResult<Map<String, ServiceLink>> deserialize(InputStream inputStream) throws IOException {
        ServiceLink[] serviceLinks = OBJECT_MAPPER.readValue(inputStream, ServiceLink[].class);
        Map<String, ServiceLink> serviceLinkList = Arrays.stream(serviceLinks).collect(Collectors.toMap(s -> (s.getServiceId() + "_" + s.getLinkId()), s -> s));
        return new ParsingResult<>(serviceLinkList, serviceLinkList.size());
    }
}

