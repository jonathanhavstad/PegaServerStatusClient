package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/4/16.
 */

public class AppsLayoutInfo extends BaseLayoutInfo {
    public static final String FRIENDLY_NAME = "Applications";

    public AppsLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        if (appData != null) {
            return appData.get(childKey);
        }
        return null;
    }

    @Override
    public String getFriendlyName() {
        return FRIENDLY_NAME;
    }

    @Override
    public String getShortName() {
        return key;
    }

    @Override
    public void setFriendlyName(String friendlyName) {

    }

    @Override
    public String getKey() {
        return FRIENDLY_NAME;
    }

    @Override
    public void setKey(String key) {

    }

    @Override
    public BaseLayoutInfo getChildLayout(int index) {
        if (index >= 0 && index < childrenLayouts.size()) {
            return childrenLayouts.get(index);
        }
        return null;
    }

    @Override
    public boolean readFromInputStream(InputStream in) {
        if (appData == null && in != null) {
            Gson gson = new Gson();
            JsonReader jsonReader = new JsonReader(new InputStreamReader(in));
            JsonArray jsonArray = gson.fromJson(jsonReader, JsonArray.class);
            List<BaseLayoutInfo> layoutList = new ArrayList<>();
            Map<String, Object> appsData = new HashMap<>();
            AppLayoutInfo layoutInfo = null;
            for (int i = 0; i < jsonArray.size(); i++) {
                if (i == 0) {
                    layoutInfo = gson.fromJson(jsonArray.get(i), MonitorAppLayoutInfo.class);
                    layoutInfo.setParentLayout(this);
                    layoutInfo.splitHeaderCols();
                    layoutInfo.splitHeaderDesc();
                    layoutInfo.setParentLayout(this);
                    layoutInfo.setKey(layoutInfo.getAppId());
                    layoutInfo.setLayoutIndex(i);
                    layoutList.add(layoutInfo);
                } else if (i == 1) {
                    layoutInfo = gson.fromJson(jsonArray.get(i), StatusAppLayoutInfo.class);
                    layoutInfo.setParentLayout(this);
                    layoutInfo.splitHeaderCols();
                    layoutInfo.splitHeaderDesc();
                    layoutInfo.setKey(layoutInfo.getAppId());
                    layoutList.add(layoutInfo);
                }
                layoutInfo.setLayoutIndex(i);
                appsData.put(layoutInfo.getAppName(), null);
            }
            setChildrenLayouts(layoutList);
            setAppData(appsData);

            return true;
        }

        if (appData != null) {
            return true;
        }

        return false;
    }

    @Override
    public List<String> getDataUrls() {
        if (dataUrls == null) {
            if (appData != null) {
                dataUrls = new ArrayList<>();
                Map<String, Object> mapAppData = (Map<String, Object>) appData;
                for (String key : mapAppData.keySet()) {
                    AppLayoutInfo childAppData = (AppLayoutInfo) mapAppData.get(key);
                    dataUrls.add(childAppData.getUrl());
                }
                return dataUrls;
            }
        }
        return dataUrls;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        if (filter != null &&
                !KeyMapping.shouldIgnoreKey(filter)) {
            for (BaseLayoutInfo baseLayoutInfo : childrenLayouts) {
                if (baseLayoutInfo.getKey().equals(filter)) {
                    return baseLayoutInfo;
                }
            }
        }
        return this;
    }

    @Override
    public int size() {
        return childrenLayouts.size();
    }
}
