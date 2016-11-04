package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class BaseLayoutInfo {
    @Expose
    @SerializedName("HeaderColumn")
    private String headerColumns;
    @Expose
    @SerializedName("HeaderDesc")
    private String headerDesc;

    protected String friendlyName;
    protected String key;

    protected String[] headerColsList;
    protected String[] headerDescList;

    private Map<String, String> headerMap;

    public String getHeaderColumns() {
        return headerColumns;
    }

    public void setHeaderColumns(String headerColumns) {
        this.headerColumns = headerColumns;
    }

    public String getHeaderDesc() {
        return headerDesc;
    }

    public void setHeaderDesc(String headerDesc) {
        this.headerDescList = headerDesc.split(",");
        this.headerDesc = headerDesc;
    }

    public void splitHeaderDesc() {
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

    public void splitHeaderCols() {
        if (headerColumns != null) {
            headerColsList = headerColumns.split(",");
            trimListElements(headerColsList);
        }
    }

    public String[] getHeaderColsList() {
        return headerColsList;
    }

    public String getFriendlyName(String key, boolean appendParent) {
        return key;
    }

    public abstract Object getValue(Map<String, Object> appData, String childKey);

    @Override
    public String toString() {
        return getFriendlyName();
    }

    public abstract String getFriendlyName();
    public abstract void setFriendlyName(String friendlyName);
    public abstract String getKey();
    public abstract void setKey(String key);
    public abstract BaseLayoutInfo createChildLayout(String parentKey);
    public abstract String getUrl();

}
