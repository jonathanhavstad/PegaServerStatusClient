package com.cisco.pegaserverstatusclient.data;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/7/16.
 */

public class BasicLayoutInfo extends BaseLayoutInfo {
    private String data;

    public BasicLayoutInfo(BaseLayoutInfo parentLayout, String key, Object data) {
        super(parentLayout);
        this.headerColumns = "\"\", \"\"";
        this.headerDesc = "\"\", \"\"";
        this.splitHeaderCols();
        this.splitHeaderDesc();
        this.key = key;
        this.data = getConcatData(data);
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
        return data;
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
        return null;
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
        return data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String getKeyedValue(int colIndex, String key) {
        if (key.equals(key)) {
            return data;
        }
        return null;
    }

    private String getConcatData(Object data) {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        if (data instanceof Map<?,?>) {
            Map<String, Object> mapData = (Map<String, Object>) data;
            for (String childKey : mapData.keySet()) {
                sb.append(childKey);
                if (index < mapData.size() - 1) {
                    sb.append("\n");
                }
                index++;
            }
        } else if (data instanceof List<?>) {
            List<String> listData = (List<String>) data;
            for (String item : listData) {
                sb.append(item);
                if (index < listData.size()) {
                    sb.append("\n");
                }
            }
        } else if (data instanceof String) {
            sb.append(data);
        } else {
            sb.append("----");
        }
        return sb.toString();
    }
}
