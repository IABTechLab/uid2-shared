package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.ServiceLink;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ServiceLinkParser implements Parser<Map<String, ServiceLink>> {

    @Override
    public ParsingResult<Map<String, ServiceLink>> deserialize(InputStream inputStream) throws IOException {
        JsonArray spec = Utils.toJsonArray(inputStream);
        final HashMap<String, ServiceLink> serviceLinkMap = new HashMap<>();
        for (int i = 0; i < spec.size(); i++) {
            JsonObject serviceLinkSpec = spec.getJsonObject(i);
            String linkId = serviceLinkSpec.getString("link_id");
            int serviceId = serviceLinkSpec.getInteger("service_id");
            int siteId = serviceLinkSpec.getInteger("site_id");
            String name = serviceLinkSpec.getString("name");

            ServiceLink serviceLink = new ServiceLink(linkId, serviceId, siteId, name);

            serviceLinkMap.put(linkId, serviceLink);
        }
        return new ParsingResult<>(serviceLinkMap, serviceLinkMap.size());
    }
}

