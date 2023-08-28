package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.model.Site;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SiteParser implements Parser<Map<Integer, Site>> {
    @Override
    public ParsingResult<Map<Integer, Site>> deserialize(InputStream inputStream) throws IOException {
        final JsonArray sitesSpec = Utils.toJsonArray(inputStream);
        final HashMap<Integer, Site> sites = new HashMap<>();
        for (int i = 0; i < sitesSpec.size(); ++i) {
            JsonObject siteSpec = sitesSpec.getJsonObject(i);
            final int siteId = siteSpec.getInteger("id");
            final String name = siteSpec.getString("name");
            final Boolean enabled = siteSpec.getBoolean("enabled", false);
            final JsonArray domainNamesJson = siteSpec.getJsonArray("domain_names");
            final Site site;
            if(domainNamesJson != null) {
                Set<String> domainNames = domainNamesJson.stream().map(String::valueOf).collect(Collectors.toSet());
                site = new Site(siteId, name, enabled, domainNames);
            } else {
                site = new Site(siteId, name, enabled);
            }
            sites.put(site.getId(), site);
        }
        return new ParsingResult<>(sites, sites.size());
    }
}