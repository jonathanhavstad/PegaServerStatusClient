package com.cisco.pegaserverstatusclient.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class AppLayoutInfo extends BaseLayoutInfo {
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
    @SerializedName("Method")
    private String method;

    public AppLayoutInfo() {
        super(null);
    }

    public AppLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
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

    @Override
    public boolean readFromNetwork(InputStream in) {
        List<BaseLayoutInfo> layoutList = new ArrayList<>();
        if (appData != null && childrenLayouts ==  null) {
            orderedKeySet = KeyMapping.populateOrderedKeySet(appData);
            for (String key : orderedKeySet) {
                LifecycleLayoutInfo lifecycleLayoutInfo = new LifecycleLayoutInfo(this);
                lifecycleLayoutInfo.setKey(key);
                lifecycleLayoutInfo.setAppData((Map<String, Object>) appData.get(key));
                lifecycleLayoutInfo.setFriendlyName(lifecycleLayoutInfo.getFriendlyName(key, false));
                lifecycleLayoutInfo.setHeaderColumns(getHeaderColumns());
                lifecycleLayoutInfo.setHeaderDesc(getHeaderDesc());
                lifecycleLayoutInfo.setLayout(layout);
                lifecycleLayoutInfo.splitHeaderCols();
                lifecycleLayoutInfo.splitHeaderDesc();
                lifecycleLayoutInfo.setKey(key);
                layoutList.add(lifecycleLayoutInfo);
            }
            setChildrenLayouts(layoutList);
            return true;
        } else if (childrenLayouts != null) {
            return true;
        }

        // TODO: Change to a REST call to read from the network

        return false;
    }

    @Override
    public List<String> getDataUrls() {
        List<String> dataUrlList = new ArrayList<>();
        dataUrlList.add(url);
        return dataUrlList;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        if (appData != null &&
                filter != null &&
                !KeyMapping.shouldIgnoreKey(filter) &&
                appData.containsKey(filter)) {
            AppLayoutInfo appLayoutInfo = new AppLayoutInfo(getParentLayout());

            ArrayList<BaseLayoutInfo> filteredChildrenLayout = new ArrayList<>();

            for (BaseLayoutInfo childLayout : childrenLayouts) {
                if (childLayout.getKey().equals(filter)) {
                    filteredChildrenLayout.add(childLayout);
                }
            }
            appLayoutInfo.setChildrenLayouts(filteredChildrenLayout);

            appLayoutInfo.setAppId(getAppId());
            appLayoutInfo.setAppName(getAppName());
            appLayoutInfo.setScreen(getScreen());
            appLayoutInfo.setMethod(getMethod());
            appLayoutInfo.setLayout(getLayout());
            appLayoutInfo.setAction(getAction());
            appLayoutInfo.setUrl(getUrl());
            appLayoutInfo.setFriendlyName(getFriendlyName());
            appLayoutInfo.setKey(getKey());
            appLayoutInfo.setHeaderColumns(getHeaderColumns());
            appLayoutInfo.setHeaderDesc(getHeaderDesc());
            appLayoutInfo.splitHeaderCols();
            appLayoutInfo.splitHeaderDesc();

            Map<String, Object> filteredAppData = new HashMap<>();
            filteredAppData.put(filter, appData.get(filter));
            appLayoutInfo.appData = filteredAppData;
            appLayoutInfo.orderedKeySet = KeyMapping.populateOrderedKeySet(filteredAppData);

            return appLayoutInfo;
        }
        return this;
    }

    @Override
    public int size() {
        return orderedKeySet.size();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return appId;
    }

    @Override
    public String getShortName() {
        if (friendlyName == null && appId != null) {
            int firstWordEndIndex = appId.indexOf(" ");
            if (firstWordEndIndex < 0) {
                firstWordEndIndex = appId.length();
            }
            return appId.substring(0, firstWordEndIndex);
        }
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
    public void setAppData(Map<String, Object> appData) {
        super.setAppData(appData);
        getParentLayout().getAppData().put(getAppName(), appData);
    }
}
