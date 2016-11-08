package com.cisco.pegaserverstatusclient.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class KeyMapping {
    public static final int NUM_LC_VALUES = 4;

    public static final String PROD_KEY = "prod";
    public static final String STAGE_KEY = "stage";
    public static final String LT_KEY = "lt";
    public static final String DEV_KEY = "dev";
    public static final String POC_KEY = "poc";

    public static final String[] LC_KEY_ORDER = new String[NUM_LC_VALUES];
    public static final Map<String, String> KEY_MAPPING = new HashMap<>();
    public static final List<String> IGNORE_LIST = new ArrayList<>();

    static {
        LC_KEY_ORDER[0] = PROD_KEY;
        LC_KEY_ORDER[1] = STAGE_KEY;
        LC_KEY_ORDER[2] = LT_KEY;
        LC_KEY_ORDER[3] = DEV_KEY;

        KEY_MAPPING.put(PROD_KEY, "Production");
        KEY_MAPPING.put(STAGE_KEY, "Stage");
        KEY_MAPPING.put(LT_KEY, "Load Test");
        KEY_MAPPING.put(DEV_KEY, "Development");

        IGNORE_LIST.add(POC_KEY);
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
