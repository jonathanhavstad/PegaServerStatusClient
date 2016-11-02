package com.cisco.pegaserverstatusclient.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class DomainLayoutInfo extends BaseLayoutInfo {
    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        if (appData != null) {
            Object childValue = appData.get(AppLayoutInfo.APP_JSON_KEY);
            if (childValue != null && childValue instanceof Map<?,?>) {
                Map<String, Object> appChildMap = (Map<String, Object>) childValue;
                if (appChildMap.containsKey(childKey)) {
                    return appChildMap;
                }
            }
            childValue = appData.get(ServerLayoutInfo.SERVER_JSON_KEY);
            if (childValue != null && childValue instanceof Map<?,?>) {
                Map<String, Object> serverChildMap = (Map<String, Object>) childValue;
                if (serverChildMap.containsKey(childKey)) {
                    return serverChildMap;
                }
            }
            return appData.get(key);
        }
        return null;
    }

    @Override
    public String getFriendlyName() {
        return friendlyName;
    }

    @Override
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public BaseLayoutInfo createChildLayout(String parentKey) {
        if (parentKey != null) {
            if (parentKey.equalsIgnoreCase(AppLayoutInfo.APP_JSON_KEY)) {
                return new AppLayoutInfo();
            } else if (parentKey.equalsIgnoreCase(ServerLayoutInfo.SERVER_JSON_KEY)) {
                return new ServerLayoutInfo();
            }
        }
        return null;
    }
}
