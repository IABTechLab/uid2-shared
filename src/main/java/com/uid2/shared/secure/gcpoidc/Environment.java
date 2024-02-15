package com.uid2.shared.secure.gcpoidc;

import com.google.common.base.Strings;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Environment {
    Production("prod"),
    Integration("integ"),
    EndToEnd("endtoend");

    @Getter
    private String name;

    private static final Map<String, Environment> ENUM_MAP;

    Environment(String name){
        this.name = name;
    }

    static{
        ENUM_MAP = Stream.of(Environment.values()).collect(Collectors.toMap(i -> i.getName(), Function.identity()));
    }

    public static Environment fromString(String str){
        if(Strings.isNullOrEmpty(str)){
            return null;
        }
        return ENUM_MAP.get(str.toLowerCase());
    }
}
