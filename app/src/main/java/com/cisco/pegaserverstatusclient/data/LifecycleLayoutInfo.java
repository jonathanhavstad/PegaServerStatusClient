package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static final String LT_FRIENDLY_NAME = "Load Test";
    public static final String DEV_FRIENDLY_NAME = "Development";

    public static final String[] LC_KEY_ORDER = new String[NUM_LC_VALUES];

    public static final Map<String, String> LC_MAPPING = new HashMap<>();

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

    public LifecycleLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
    }

    public String getFriendlyName(String key, boolean appendParent) {
        if (LC_MAPPING.containsKey(key.toLowerCase())) {
            return LC_MAPPING.get(key.toLowerCase());
        }
        return null;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    @Override
    public String getShortName() {
        return key;
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

    @Override
    public BaseLayoutInfo createChildLayout(String parentKey) {
        return new DomainLayoutInfo(this);
    }

    @Override
    public BaseLayoutInfo getChildLayout(int index) {
        if (index >= 0 && index < childrenLayouts.size()) {
            return childrenLayouts.get(index);
        }
        return null;
    }

    @Override
    public boolean readFromNetwork(InputStream in) {
        List<BaseLayoutInfo> layoutList = new ArrayList<>();

        if (appData != null) {
            orderedKeySet = KeyMapping.populateOrderedKeySet(appData);
            for (String key : orderedKeySet) {
                DomainLayoutInfo layoutInfo = new DomainLayoutInfo(this,
                        (Map<String, Object>) appData.get(key),
                        key,
                        1);
                layoutList.add(layoutInfo);
            }
            setChildrenLayouts(layoutList);
            return true;
        }

        return false;
    }

    @Override
    public List<String> getDataUrls() {
        return null;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        for (BaseLayoutInfo childLayout : childrenLayouts) {
            if (childLayout.getKey().equals(filter)) {
                return childLayout;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return orderedKeySet.size() * headerColsList.length;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }
}
