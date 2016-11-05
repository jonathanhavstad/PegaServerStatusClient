package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class DomainAppLayoutInfo extends BaseLayoutInfo {
    public static final String APP_JSON_KEY = "APPS";

    public DomainAppLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
    }

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
        return new ServerLayoutInfo(this);
    }

    @Override
    public BaseLayoutInfo getChildLayout(int index) {
        return null;
    }

    @Override
    public boolean readFromNetwork(InputStream in) {
        return false;
    }

    @Override
    public List<String> getDataUrls() {
        return null;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        return null;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        if (appData != null) {
            Object childValue = appData.get(ServerLayoutInfo.SERVER_JSON_KEY);
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
}
