package com.uid2.shared.secure.gcpoidc;

import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum IdentityScope {
    UID2("uid2"),
    EUID("euid");

    @Getter
    private String name;

    private static final Map<String, IdentityScope> ENUM_MAP;

    IdentityScope(String name){
        this.name = name;
    }

    static{
        ENUM_MAP = Stream.of(IdentityScope.values()).collect(Collectors.toMap(i -> i.getName(), Function.identity()));
    }

    public static IdentityScope fromString(String str){
        return ENUM_MAP.get(str.toLowerCase());
    }
}
