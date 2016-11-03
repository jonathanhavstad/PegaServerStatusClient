package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaParentFragment;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
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

    @Override
    public BaseLayoutInfo createChildLayout(String parentKey) {
        return new DomainLayoutInfo();
    }

    @Override
    public PegaBaseFragment addLayoutToView(Context context,
                                            String parentKey,
                                            ArrayList<String> keyPath,
                                            Object appData,
                                            AddLayoutViewAdapter addLayoutViewAdapter) {
        if (addLayoutViewAdapter != null) {
            if (appData instanceof Map<?, ?>) {
                Map<String, Object> mapAppData = (Map<String, Object>) appData;
                int index = 0;
                for (String lcKey : LifecycleLayoutInfo.LC_KEY_ORDER) {
                    if (mapAppData.containsKey(lcKey.toLowerCase())) {
                        PegaParentFragment fragment =
                                PegaParentFragment
                                        .newInstance(context,
                                                LifecycleLayoutInfo.LC_MAPPING.get(lcKey),
                                                lcKey,
                                                (ArrayList<String>) keyPath.clone(),
                                                mapAppData.get(lcKey));
                        addLayoutViewAdapter.add(fragment);
                    }
                    if (index == 0) {
                        key = lcKey;
                    }
                    index++;
                }
            }
        }

        return null;
    }

    @Override
    public PegaBaseFragment replaceLayoutToView(Context context,
                                                String parentKey,
                                                ArrayList<String> keyPath,
                                                Object appData,
                                                ReplaceLayoutViewAdapter replaceLayoutViewAdapter) {
        if (replaceLayoutViewAdapter != null) {
            if (appData instanceof Map<?, ?>) {
                if (LifecycleLayoutInfo.LC_MAPPING.containsKey(key)) {
                    replaceLayoutViewAdapter.replace(false, null);
                }
            }
        }

        return null;
    }
}
