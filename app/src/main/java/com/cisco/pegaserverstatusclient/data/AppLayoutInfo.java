package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
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
    @SerializedName("Layout")
    private String layout;
    @Expose
    @SerializedName("Method")
    private String method;
    @Expose
    @SerializedName("URL")
    private String url;
    @Expose
    @SerializedName("HeaderColumn")
    private String headerColumns;
    @Expose
    @SerializedName("HeaderDesc")
    private String headerDesc;

    private String[] headerColsList;
    private String[] headerDescList;

    private Map<String, String> headerMap;

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

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeaderColumns() {
        return headerColumns;
    }

    public void setHeaderColumns(String headerColumns) {
        this.headerColumns = headerColumns;
    }

    public void setHeaderColsList() {
        if (headerColumns != null) {
            headerColsList = headerColumns.split(",");
            trimListElements(headerColsList);
        }
    }

    public String[] getHeaderColsList() {
        return headerColsList;
    }

    public String getHeaderDesc() {
        return headerDesc;
    }

    public void setHeaderDesc(String headerDesc) {
        this.headerDescList = headerDesc.split(",");
        this.headerDesc = headerDesc;
    }

    public void setHeaderDescList() {
        if (headerDesc != null) {
            headerDescList = headerDesc.split(",");
            trimListElements(headerDescList);
        }
    }

    private void trimListElements(String[] list) {
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim();
        }
    }

    public String[] getHeaderDescList() {
        return headerDescList;
    }

    public void createHeaderMap() {
        if (headerMap == null) {
            headerMap = new HashMap<String, String>();
        }
        headerMap.clear();
        for (int i = 0; i < this.headerColsList.length; i++) {
            if (i < this.headerDescList.length) {
                headerMap.put(headerColsList[i], headerDescList[i]);
            }
        }
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return null;
    }

    @Override
    public void setFriendlyName(String friendlyName) {

    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public void setKey(String key) {

    }

    @Override
    public BaseLayoutInfo createChildLayout(String parentKey) {
        return null;
    }

    @Override
    public PegaBaseFragment addLayoutToView(Context context, String parentKey, ArrayList<String> keyPath, Object appData, AddLayoutViewAdapter addLayoutViewAdapter) {
        return null;
    }

    @Override
    public PegaBaseFragment replaceLayoutToView(Context context, String parentKey, ArrayList<String> keyPath, Object appData, ReplaceLayoutViewAdapter replaceLayoutViewAdapter) {
        return null;
    }
}
