package com.cisco.pegaserverstatusclient.layouts;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public abstract class AppLayoutInfo extends BaseLayoutInfo {
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
    public List<String> getDataUrls() {
        List<String> dataUrlList = new ArrayList<>();
        dataUrlList.add(url);
        return dataUrlList;
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
    public BaseLayoutInfo getChildLayout(int index) {
        if (index >= 0 && index < childrenLayouts.size()) {
            return childrenLayouts.get(index);
        }
        return null;
    }

    @Override
    public void setAppData(Object appData) {
        super.setAppData(appData);
        Map<String, Object> parentAppData = (Map<String, Object>) getParentLayout().getAppData();
        parentAppData.put(getAppName(), appData);
    }

    @Override
    public boolean forceDrawerLayout() {
        return true;
    }
}
