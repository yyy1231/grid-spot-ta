package com.novax.testcase;

import com.novax.util.PropertyUtil;
import com.novax.util.StringToMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PublicParams {
    public String baseUrl = "http://" + PropertyUtil.getServerUrl() + ":" + PropertyUtil.getServerPort();
    public static List<String> allAttributeNameList = new ArrayList<>();
    public static List<String> autoStartUpAttributeNameList = new ArrayList<>();
    public static List<String> clearDataAttributeNameList = new ArrayList<>();
    public String headers = "{language=en,userId=28678286,Content-type=application/json}";
    public HashMap<String, String> reqHeaders = StringToMap.stringToMap(headers);
}
