package com.uid2.shared.auth;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Roles {
    public static <T extends Enum<T>> Set<T> getRoles(Class<T> type, JsonObject clientKeyJson) {
        return getRoles(type, clientKeyJson.getJsonArray("roles", new JsonArray()));
    }

    public static <T extends Enum<T>> Set<T> getRoles(Class<T> type, JsonArray rolesJsonArray) {
        Set<T> roles = new HashSet<>();
        for (int i = 0; i < rolesJsonArray.size(); ++i) {
            String value = rolesJsonArray.getString(i).toUpperCase();
            roles.add(Enum.valueOf(type, value));
        }
        return roles;
    }

    public static <T extends Enum<T>> Set<T> getRoles(Class<T> type, String rolesSpec) {
        return Arrays.stream(rolesSpec.split(","))
                .map(r -> r.trim().toUpperCase())
                .map(r -> Enum.valueOf(type, r))
                .collect(Collectors.toSet());
    }

    public static <T> String getRolesString(Set<T> roles) {
        return String.join(",", roles.stream().map(Object::toString).collect(Collectors.toList()));
    }

}
