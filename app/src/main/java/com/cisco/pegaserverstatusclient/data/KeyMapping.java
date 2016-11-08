package com.cisco.pegaserverstatusclient.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class KeyMapping {
    public static final Map<String, String> KEY_MAPPING = new HashMap<>();
    public static final List<String> IGNORE_LIST = new ArrayList<>();

    static {


        KEY_MAPPING.put("prod", "Production");
        KEY_MAPPING.put("stage", "Stage");
        KEY_MAPPING.put("lt", "Load Test");
        KEY_MAPPING.put("dev", "Development");

        IGNORE_LIST.add("poc");
    }

    public static String getFriendlyName(String key) {
        for (String keyMapping : KEY_MAPPING.keySet()) {
            if (key.equalsIgnoreCase(keyMapping)) {
                return KEY_MAPPING.get(keyMapping);
            }
        }
        if (!shouldIgnoreKey(key)) {
            return key;
        }
        return null;
    }

    public static boolean shouldIgnoreKey(String key) {
        for (String ignoreKey : IGNORE_LIST) {
            if (ignoreKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> populateOrderedKeySet(Map<String, Object> appData) {
        List<String> orderedKeySet = new ArrayList<>();
        if (appData != null) {
            for (String key : appData.keySet()) {
                if (!shouldIgnoreKey(key)) {
                    orderedKeySet.add(key);
                }
            }
        }
        return orderedKeySet;
    }
}
