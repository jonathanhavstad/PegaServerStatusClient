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

    @Override
    public String getShortName() {
        return key;
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
    public int size() {
        return 0;
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

    @Override
    public String getKeyedValue(int colIndex, String key) {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        Object childAppData = appData.get(key);
        if (childAppData instanceof Map<?,?>) {
            Map<String, Object> childAppMapData = (Map<String, Object>) appData;
            for (String childKey : childAppMapData.keySet()) {
                sb.append(childKey);
                if (index < childAppMapData.size() - 1) {
                    sb.append("\n");
                }
                index++;
            }
        } else if (childAppData instanceof List<?>) {
            List<String> childListData = (List<String>) childAppData;
            for (String value : childListData) {
                sb.append(value);
                if (index < childListData.size() - 1) {
                    sb.append("\n");
                }
                index++;
            }
        } else if (childAppData instanceof String) {
            sb.append((String) childAppData);
        }

        return sb.toString();
    }
}
