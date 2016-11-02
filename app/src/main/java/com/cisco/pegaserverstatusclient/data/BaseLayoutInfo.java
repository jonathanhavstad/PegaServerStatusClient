package com.cisco.pegaserverstatusclient.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class BaseLayoutInfo {
    @Expose
    @SerializedName("HeaderColumn")
    protected String headerCols;
    @Expose
    @SerializedName("HeaderDesc")
    protected String headerDesc;
    protected String[] headerColsList;
    protected String[] headerDescList;
    protected Map<String, String> headerMap;
    protected String friendlyName;
    protected String key;

    public String getHeaderCols() {
        return headerCols;
    }

    public void setHeaderCols(String headerCols) {
        this.headerCols = headerCols;
        this.headerColsList = headerCols.split(",");
    }

    public String getHeaderDesc() {
        return headerDesc;
    }

    public void setHeaderDesc(String headerDesc) {
        this.headerDesc = headerDesc;
        this.headerDescList = headerDesc.split(",");
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
}
