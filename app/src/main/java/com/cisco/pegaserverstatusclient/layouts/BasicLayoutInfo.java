package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import java.io.InputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/7/16.
 */

public class BasicLayoutInfo extends BaseLayoutInfo {
    public BasicLayoutInfo(BaseLayoutInfo parentLayout, String key, String data) {
        super(parentLayout);
        this.key = key;
        this.appData = data;
        this.friendlyName = KeyMapping.getFriendlyName(key);
        this.orderedKeySet = new ArrayList<>();
        this.orderedKeySet.add(data);
        this.childrenLayouts = new ArrayList<>();
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return friendlyName;
    }

    @Override
    public String getShortName() {
        return appData.toString();
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
    public BaseLayoutInfo getChildLayout(int index) {
        return this;
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
        return 1;
    }

    @Override
    public int getKeyIndex(String key) {
        return 0;
    }

    @Override
    public String toString() {
        return appData.toString();
    }

    @Override
    public String getKeyedValue(int colIndex, String key, boolean headerIsKey) {
        return appData.toString();
    }

    @Override
    public int getNumCols() {
        return orderedKeySet.size();
    }
}
