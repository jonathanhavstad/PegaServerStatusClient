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
    public static final String APPS_KEY = "APPS";
    public static final String HOSTS_KEY = "HOSTS";
    public static final String DATETIME_KEY = "DateTime";
    public static final String STATUS_KEY = "Status";
    public static final String PROXY_URL_KEY = "ProxyURL";
    public static final String STATUS_ID_KEY = "_id";
    public static final String STATUS_WORK_KEY = "pyStatusWork";
    public static final String FILE_URI_KEY = "file://";
    public static final String WEB_URI_KEY = "https://";

    public static final String GRID_LAYOUT_KEY = "GRID";
    public static final String VERTICAL_LAYOUT_KEY = "VERTICAL";

    public static final String[] LC_KEY_ORDER = new String[NUM_LC_VALUES];
    public static final Map<String, String> KEY_MAPPING = new HashMap<>();
    public static final Map<String, String> HEADER_MAPPING = new HashMap<>();
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
        KEY_MAPPING.put(APPS_KEY, "Applications");
        KEY_MAPPING.put(HOSTS_KEY, "Hosts");
        KEY_MAPPING.put(DATETIME_KEY, "Date & Time");
        KEY_MAPPING.put(STATUS_KEY, "Status");
        KEY_MAPPING.put(PROXY_URL_KEY, "Proxy URL");

        HEADER_MAPPING.put(APPS_KEY, "Application");
        HEADER_MAPPING.put(STATUS_KEY, "Status");
        HEADER_MAPPING.put(DATETIME_KEY, "Date & Time");

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

    public static String getHeaderName(String key) {
        for (String keyMapping : HEADER_MAPPING.keySet()) {
            if (key.equalsIgnoreCase(keyMapping)) {
                return HEADER_MAPPING.get(keyMapping);
            }
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
