package com.cisco.pegaserverstatusclient.data;

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
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }
}
