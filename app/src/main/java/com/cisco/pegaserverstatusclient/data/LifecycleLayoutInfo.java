package com.cisco.pegaserverstatusclient.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

    @Expose
    @SerializedName("AppId")
    private String appId;
    @Expose
    @SerializedName("AppName")
    private String appName;
    @Expose
    @SerializedName("Action")
    private String action;
    @Expose
    @SerializedName("Screen")
    private String screen;
    @Expose
    @SerializedName("Layout")
    private String layout;
    @Expose
    @SerializedName("Method")
    private String method;
    @Expose
    @SerializedName("URL")
    private String url;

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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
}
