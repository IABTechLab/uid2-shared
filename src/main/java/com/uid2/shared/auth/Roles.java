// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared.auth;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Roles {
    public static <T extends Enum<T>> Set<T> getRoles(Class<T> type, JsonObject clientKeyJson) {
        return getRoles(type, clientKeyJson.getJsonArray("roles"));
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
