package com.uid2.shared.secure;

import java.util.Map;

public class JwtUtils {
    public static<T> T tryGetField(Map payload, String key, Class<T> clazz){
        if(payload == null){
            return null;
        }
        var rawValue = payload.get(key);
        return tryConvert(rawValue, clazz);
    }

    public static<T> T tryConvert(Object obj, Class<T> clazz){
        if(obj == null){
            return null;
        }
        try{
            return clazz.cast(obj);
        }
        catch (ClassCastException e){
            return null;
        }
    }
}
