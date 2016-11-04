package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class ServerLayoutInfo extends BaseLayoutInfo {
    public static final String SERVER_JSON_KEY = "HOSTS";

    public String getFriendlyName() {
        return friendlyName;
    }

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
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }
}
