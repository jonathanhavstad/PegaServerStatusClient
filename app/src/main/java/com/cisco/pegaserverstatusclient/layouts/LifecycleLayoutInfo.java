package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/26/16.
 */

public class LifecycleLayoutInfo extends BaseLayoutInfo {

    public LifecycleLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
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
    public BaseLayoutInfo getChildLayout(int index) {
        if (index >= 0 && index < childrenLayouts.size()) {
            return childrenLayouts.get(index);
        }
        return null;
    }

    @Override
    public boolean readFromNetwork(InputStream in) {
        List<BaseLayoutInfo> layoutList = new ArrayList<>();
        Map<String, Object> mapAppData = (Map<String, Object>) appData;

        if (appData != null) {
            orderedKeySet = KeyMapping.populateOrderedKeySet(mapAppData);
            for (String key : orderedKeySet) {
                ContentDetailLayout layoutInfo = new ContentDetailLayout(this,
                        (Map<String, Object>) mapAppData.get(key),
                        key,
                        headerColsList,
                        headerDescList,
                        headerColsList.length);
                layoutList.add(layoutInfo);
            }
            setChildrenLayouts(layoutList);
            return true;
        }

        return false;
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
        return orderedKeySet.size();
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }

    public String getFriendlyName(String key, boolean appendParent) {
        return KeyMapping.getFriendlyName(key);
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
