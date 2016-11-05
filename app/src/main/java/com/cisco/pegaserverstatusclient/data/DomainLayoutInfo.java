package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class DomainLayoutInfo extends BaseLayoutInfo {
    public DomainLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);

        // TODO: Remove these when the app layout become available on the network
        headerColumns = "APPS, STATUS, DateTime, ProxyURL";
        headerDesc = "Applications, Status, Date & Time, Proxy URL";
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        if (appData != null) {
            Object childValue = appData.get(DomainAppLayoutInfo.APP_JSON_KEY);
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
            if (parentKey.equalsIgnoreCase(DomainAppLayoutInfo.APP_JSON_KEY)) {
                return new DomainAppLayoutInfo(this);
            } else if (parentKey.equalsIgnoreCase(ServerLayoutInfo.SERVER_JSON_KEY)) {
                return new ServerLayoutInfo(this);
            }
        }
        return null;
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
}
