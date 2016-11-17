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
    public static class LayoutPreferences {
        public boolean isBold;
        public boolean isClickable;
        public boolean isUnderlined;

        public boolean isBold() {
            return isBold;
        }

        public void setBold(boolean bold) {
            isBold = bold;
        }

        public boolean isClickable() {
            return isClickable;
        }

        public void setClickable(boolean clickable) {
            isClickable = clickable;
        }

        public boolean isUnderlined() {
            return isUnderlined;
        }

        public void setUnderlined(boolean underlined) {
            isUnderlined = underlined;
        }
    }

    public static LayoutPreferences DEFAULT_LAYOUT_PREFERENCES;

    static {
        DEFAULT_LAYOUT_PREFERENCES = new LayoutPreferences();
        DEFAULT_LAYOUT_PREFERENCES.setBold(false);
        DEFAULT_LAYOUT_PREFERENCES.setClickable(false);
        DEFAULT_LAYOUT_PREFERENCES.setUnderlined(false);
    }

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

    protected Object appData;

    protected List<BaseLayoutInfo> childrenLayouts;

    protected List<String> orderedKeySet;

    protected List<String> dataUrls;

    private BaseLayoutInfo parentLayout;

    protected boolean shouldBeParent;

    protected int layoutIndex;

    public BaseLayoutInfo(BaseLayoutInfo parentLayout) {
        this.parentLayout = parentLayout;
        this.shouldBeParent = true;
        this.layoutIndex = -1;
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

    public void setAppData(Object appData) {
        this.appData = appData;
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            this.orderedKeySet = KeyMapping.populateOrderedKeySet(mapAppData);
        }
    }

    public Object getAppData() {
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

    public Object getKeyedValue(int colIndex, String key, boolean headerIsKey) {
        Object keyedValue = key;
        if (colIndex == 0 && headerIsKey) {
            keyedValue = key;
        } else if (appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            if (headerColsList != null) {
                String header = headerColsList[colIndex];
                for (String headerKey : mapAppData.keySet()) {
                    if (headerKey.equalsIgnoreCase(header)) {
                        keyedValue = mapAppData.get(headerKey);
                    }
                }
            } else {
                int index = 0;
                for (String childkey : mapAppData.keySet()) {
                    if (index == colIndex) {
                        keyedValue = mapAppData.get(childkey);
                    }
                    index++;
                }
            }
        }

        return keyedValue;
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
        int index = 0;
        if (headerColsList != null) {
            index = position / headerColsList.length;
        } else if (appData != null && appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            index = position / mapAppData.size();
        }
        return orderedKeySet.get(index);
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

    public LayoutPreferences getLayoutPrefrences(int childIndex) {
        return DEFAULT_LAYOUT_PREFERENCES;
    }

    public int getNumCols() {
        if (headerColsList != null) {
            return headerColsList.length;
        }
        return 0;
    }

    public boolean isShouldBeParent() {
        return shouldBeParent;
    }

    public void setShouldBeParent(boolean shouldBeParent) {
        this.shouldBeParent = shouldBeParent;
    }

    public abstract Object getValue(Map<String, Object> appData, String childKey);
    public abstract String getFriendlyName();
    public abstract String getShortName();
    public abstract void setFriendlyName(String friendlyName);
    public abstract String getKey();
    public abstract void setKey(String key);
    public abstract BaseLayoutInfo getChildLayout(int index);
    public abstract boolean readFromInputStream(InputStream in);
    public abstract List<String> getDataUrls();
    public abstract BaseLayoutInfo filteredLayout(String filter);
    public abstract int size();

    @Override
    public String toString() {
        return getFriendlyName();
    }

    public static String concatListData(List<String> listData) {
        StringBuffer sb = new StringBuffer();
        int itemIndex = 0;
        for (String item :listData) {
            sb.append(item);
            if (itemIndex < listData.size() - 1) {
                sb.append("\n");
            }
            itemIndex++;
        }
        return sb.toString();
    }

    public boolean forceDrawerLayout() {
        return false;
    }

    public int getLayoutIndex() {
        return layoutIndex;
    }

    public void setLayoutIndex(int layoutIndex) {
        this.layoutIndex = layoutIndex;
    }

    public static BaseLayoutInfo getRoot(BaseLayoutInfo currentLayoutInfo) {
        BaseLayoutInfo root = currentLayoutInfo;

        while (root.getParentLayout() != null) {
            root = root.getParentLayout();
        }

        return root;
    }
}
