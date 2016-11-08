package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class BaseLayoutInfo {
    @Expose
    @SerializedName("Layout")
    protected String layout;
    @Expose
    @SerializedName("HeaderColumn")
    protected String headerColumns;
    @Expose
    @SerializedName("HeaderDesc")
    protected String headerDesc;
    @Expose
    @SerializedName("URL")
    protected String url;

    protected String friendlyName;
    protected String key;

    protected String[] headerColsList;
    protected String[] headerDescList;

    private Map<String, String> headerMap;

    protected Map<String, Object> appData;

    protected List<BaseLayoutInfo> childrenLayouts;

    protected List<String> orderedKeySet;

    protected List<String> dataUrls;

    private BaseLayoutInfo parentLayout;

    public BaseLayoutInfo(BaseLayoutInfo parentLayout) {
        this.parentLayout = parentLayout;
    }

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
        this.headerDesc = headerDesc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public void splitHeaderDesc() {
        if (headerDesc != null) {
            headerDescList = headerDesc.split(",");
            trimListElements(headerDescList);
        }
    }

    public void splitHeaderCols() {
        if (headerColumns != null) {
            headerColsList = headerColumns.split(",");
            trimListElements(headerColsList);
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

    public String[] getHeaderColsList() {
        return headerColsList;
    }

    public String getFriendlyName(String key, boolean appendParent) {
        return key;
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

    public void setAppData(Map<String, Object> appData) {
        this.appData = appData;
        this.orderedKeySet = KeyMapping.populateOrderedKeySet(appData);
    }

    public Map<String, Object> getAppData() {
        return appData;
    }

    public void setChildrenLayouts(List<BaseLayoutInfo> childrenLayouts) {
        this.childrenLayouts = childrenLayouts;
    }

    public List<BaseLayoutInfo> getChildrenLayouts() {
        return childrenLayouts;
    }

    public BaseLayoutInfo getParentLayout() {
        return parentLayout;
    }

    public void setParentLayout(BaseLayoutInfo parentLayout) {
        this.parentLayout = parentLayout;
    }

    public String getKeyedValue(int colIndex, String key) {
        if (colIndex == 0) {
            return key;
        }

        String header = headerColsList[colIndex];
        for (String headerKey : appData.keySet()) {
            if (headerKey.equalsIgnoreCase(header)) {
                return appData.get(headerKey).toString();
            }
        }

        return null;
    }

    public boolean isColBold(int colIndex) {
        if (colIndex == 0) {
            return true;
        }
        return false;
    }

    public boolean isClickable(int colIndex) {
        if (colIndex == 0) {
            return true;
        }
        return false;
    }

    public int getKeyIndex(String key) {
        if (orderedKeySet.contains(key)) {
            return orderedKeySet.indexOf(key);
        }
        return -1;
    }

    public List<String> getOrderedKeySet() {
        return orderedKeySet;
    }

    public void setOrderedKeySet(List<String> orderedKeySet) {
        this.orderedKeySet = orderedKeySet;
    }

    public BaseLayoutInfo getDetailLayout(int position) {
        return getChildLayout(position);
    }

    public String getKeyFromPosition(int position) {
        return orderedKeySet.get(position / headerColsList.length);
    }

    public boolean isGridLayout() {
        if (layout != null && layout.equalsIgnoreCase("GRID")) {
            return true;
        }
        return false;
    }

    public boolean isVerticalLayout() {
        if (layout != null && layout.equalsIgnoreCase("VERTICAL")) {
            return true;
        }
        return false;
    }

    public boolean isHorizontalLayout() {
        if (layout != null && layout.equalsIgnoreCase("HORIZONTAL")) {
            return true;
        }
        return false;
    }

    public abstract Object getValue(Map<String, Object> appData, String childKey);
    public abstract String getFriendlyName();
    public abstract String getShortName();
    public abstract void setFriendlyName(String friendlyName);
    public abstract String getKey();
    public abstract void setKey(String key);
    public abstract BaseLayoutInfo createChildLayout(String parentKey);
    public abstract BaseLayoutInfo getChildLayout(int index);
    public abstract boolean readFromNetwork(InputStream in);
    public abstract List<String> getDataUrls();
    public abstract BaseLayoutInfo filteredLayout(String filter);
    public abstract int size();

    @Override
    public String toString() {
        return getFriendlyName();
    }
}
