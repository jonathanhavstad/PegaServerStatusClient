package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class ContentDetailLayout extends BaseLayoutInfo {
    private int size;
    private List<LayoutPreferences> layoutPreferencesList = new ArrayList<>();

    public ContentDetailLayout(BaseLayoutInfo parentLayoutInfo,
                               Map<String, Object> appData,
                               String key,
                               String[] headerColsList,
                               String[] headerDescList,
                               int size) {
        super(parentLayoutInfo);
        setKey(key);
        setAppData(appData);
        setFriendlyName(KeyMapping.getFriendlyName(key));
        setLayout("VERTICAL");
        this.headerColsList = headerColsList;
        this.headerDescList = headerDescList;
        this.size = size;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childKey) {
        if (appData != null) {
            Object childValue = appData.get(DomainAppLayoutInfo.APP_JSON_KEY);
            if (childValue != null && childValue instanceof Map<?,?>) {
                Map<String, Object> appChildMap = (Map<String, Object>) childValue;
                if (appChildMap.containsKey(childKey)) {
                    return appChildMap;
                }
            }
            childValue = appData.get(ServerLayoutInfo.SERVER_JSON_KEY);
            if (childValue != null && childValue instanceof Map<?,?>) {
                Map<String, Object> serverChildMap = (Map<String, Object>) childValue;
                if (serverChildMap.containsKey(childKey)) {
                    return serverChildMap;
                }
            }
            return appData.get(key);
        }
        return null;
    }

    @Override
    public String getFriendlyName() {
        return friendlyName;
    }

    @Override
    public String getShortName() {
        return key;
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
    public boolean readFromNetwork(InputStream in) {
        populateChildrenData();
        this.size = childrenLayouts.size();
        return true;
    }

    @Override
    public List<String> getDataUrls() {
        return null;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        for (BaseLayoutInfo childLayout : childrenLayouts) {
            if (childLayout.getKey().equals(filter)) {
                return childLayout;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    private void populateChildrenData() {
        ArrayList<BaseLayoutInfo> childrenList = new ArrayList<>();
        BaseLayoutInfo childLayoutInfo = null;
        orderedKeySet = new ArrayList<>();
        Map<String, Object> mapAppData = (Map<String, Object>) appData;
        for (String childKey : mapAppData.keySet()) {
            orderedKeySet.add(childKey);
            Object data = mapAppData.get(childKey);
            if (data instanceof Map<?,?>) {
                Map<String, Object> childMapAppData = (Map<String, Object>) data;

                String[] childHeaderColsList = headerColsList;
                String[] childHeaderDescList = headerDescList;

                if (childKey.equalsIgnoreCase(KeyMapping.APPS_KEY)) {
                    childHeaderColsList = headerColsList.clone();
                    childHeaderColsList[0] = childKey;
                    childHeaderDescList = headerDescList.clone();
                    childHeaderDescList[0] = KeyMapping.getHeaderName(childKey);
                }

                childLayoutInfo = new ContentDetailLayout(this,
                        childMapAppData,
                        childKey,
                        childHeaderColsList,
                        childHeaderDescList,
                        childHeaderColsList.length);
                childrenList.add(childLayoutInfo);
            } else if (data instanceof List<?>) {
                List<String> listData = (List<String>) data;
                String childData = BaseLayoutInfo.concatListData(listData);
                childLayoutInfo = new BasicLayoutInfo(this,
                        childKey,
                        childData);
                childrenList.add(childLayoutInfo);
            } else if (data instanceof String) {
                childLayoutInfo = new BasicLayoutInfo(this,
                        childKey,
                        (String) data);
                childrenList.add(childLayoutInfo);
            } else {
                String emptyValue = "";
                childLayoutInfo = new BasicLayoutInfo(this,
                        childKey,
                        emptyValue);
                childrenList.add(childLayoutInfo);
            }
        }
        setChildrenLayouts(childrenList);
    }

    @Override
    public LayoutPreferences getLayoutPrefrences(int childIndex) {
        if (childIndex >= 0 && childIndex < layoutPreferencesList.size()) {
            return layoutPreferencesList.get(childIndex);
        }
        return super.getLayoutPrefrences(childIndex);
    }
}
