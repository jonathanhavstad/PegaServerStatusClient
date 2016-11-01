package com.cisco.pegaserverstatusclient.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/26/16.
 */

public class LifecycleLayoutInfo extends BaseLayoutInfo {
    public static final int NUM_LC_VALUES = 4;

    public static final String PROD_KEY = "prod";
    public static final String STAGE_KEY = "stage";
    public static final String LT_KEY = "lt";
    public static final String DEV_KEY = "dev";

    public static final String PROD_FRIENDLY_NAME = "Production";
    public static final String STAGE_FRIENDLY_NAME = "Stage";
    public static final String LT_FRIENDLY_NAME = "Load Testing";
    public static final String DEV_FRIENDLY_NAME = "Development";

    public static final String[] LC_KEY_ORDER = new String[NUM_LC_VALUES];

    public static final Map<String, String> LC_MAPPING = new HashMap<>();

    private String key;
    private int keyIndex = 0;

    static {
        LC_KEY_ORDER[0] = PROD_KEY;
        LC_KEY_ORDER[1] = STAGE_KEY;
        LC_KEY_ORDER[2] = LT_KEY;
        LC_KEY_ORDER[3] = DEV_KEY;

        LC_MAPPING.put(PROD_KEY, PROD_FRIENDLY_NAME);
        LC_MAPPING.put(STAGE_KEY, STAGE_FRIENDLY_NAME);
        LC_MAPPING.put(LT_KEY, LT_FRIENDLY_NAME);
        LC_MAPPING.put(DEV_KEY, DEV_FRIENDLY_NAME);
    }

    public String getFriendlyName(String key, boolean appendParent) {
        if (LC_MAPPING.containsKey(key.toLowerCase())) {
            return LC_MAPPING.get(key.toLowerCase());
        }
        return null;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    @Override
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
