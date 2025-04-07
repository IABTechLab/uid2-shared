package com.uid2.shared.store.parser;

import com.uid2.shared.Utils;
import com.uid2.shared.auth.Role;
import com.uid2.shared.model.Service;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ServiceParser implements Parser<Map<Integer, Service>> {
    @Override
    public ParsingResult<Map<Integer, Service>> deserialize(InputStream inputStream) throws IOException {
        JsonArray spec = Utils.toJsonArray(inputStream);

        final HashMap<Integer, Service> serviceMap = new HashMap<>();

        for (int i = 0; i < spec.size(); i++) {
            JsonObject serviceSpec = spec.getJsonObject(i);
            int serviceId = serviceSpec.getInteger("service_id");
            int siteId = serviceSpec.getInteger("site_id");
            String name = serviceSpec.getString("name");

            JsonArray rolesSpec = serviceSpec.getJsonArray("roles");
            HashSet<Role> roles = new HashSet<>();
            for (int j = 0; j < rolesSpec.size(); j++) {
                roles.add(Enum.valueOf(Role.class, rolesSpec.getString(j)));
            }

            Service service = new Service(serviceId, siteId, name, roles);

            serviceMap.put(serviceId, service);
        }

        return new ParsingResult<>(serviceMap, serviceMap.size());
    }
}
