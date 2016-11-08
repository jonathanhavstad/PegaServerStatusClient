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
    private int size;

    public DomainLayoutInfo(BaseLayoutInfo parentLayoutInfo) {
        super(parentLayoutInfo);

        // TODO: Remove these when the app layout become available on the network
        headerColumns = "APPS, STATUS, DateTime, ProxyURL";
        headerDesc = "Applications, Status, Date & Time, Proxy URL";

        splitHeaderCols();
        splitHeaderDesc();
        size = 1;
    }

    public DomainLayoutInfo(BaseLayoutInfo parentLayoutInfo,
                            Map<String, Object> appData,
                            String key,
                            int size) {
        this(parentLayoutInfo);
        setKey(key);
        setAppData(appData);
        setFriendlyName(getFriendlyName(key, false));
        setLayout("GRID");
        splitHeaderCols();
        splitHeaderDesc();
        this.size = size;
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
    public String getShortName() {
        return key;
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
        if (index >= 0 && index < childrenLayouts.size()) {
            return childrenLayouts.get(index);
        }
        return null;
    }

    @Override
    public boolean readFromNetwork(InputStream in) {
        ArrayList<BaseLayoutInfo> filteredChildrenLayout = new ArrayList<>();

        for (String childKey : headerColsList) {
            BasicLayoutInfo childLayoutInfo = new BasicLayoutInfo(this, childKey, appData.get(childKey));
            filteredChildrenLayout.add(childLayoutInfo);
        }
        setChildrenLayouts(filteredChildrenLayout);

        return true;
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
        return size;
    }

    @Override
    public BaseLayoutInfo getDetailLayout(int position) {
        return new DomainLayoutInfo(getParentLayout(),
                appData,
                getKey(),
                headerColsList.length);
    }

    @Override
    public boolean isColBold(int colIndex) {
        return false;
    }

    @Override
    public boolean isClickable(int colIndex) {
        return false;
    }

    @Override
    public String getKeyFromPosition(int position) {
        return orderedKeySet.get(position);
    }
}
