package com.novax.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JsonStringToMap {
    public static HashMap<String, String> jsonStringToMap(String param) {
        HashMap<String, String> map = new HashMap<>();
        JSONObject jsonObject = JSON.parseObject(param);
        for (Map.Entry<String,Object> entry:jsonObject.entrySet()) {
            map.put(entry.getKey(), (String) entry.getValue());
        }
        return map;
    }
}
