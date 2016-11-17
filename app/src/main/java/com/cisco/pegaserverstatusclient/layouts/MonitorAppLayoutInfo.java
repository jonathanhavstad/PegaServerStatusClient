package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/14/16.
 */

public class MonitorAppLayoutInfo extends AppLayoutInfo {
    public MonitorAppLayoutInfo() { super(null); }

    public MonitorAppLayoutInfo(BaseLayoutInfo parentLayoutInfo) {
        super(parentLayoutInfo);
    }

    @Override
    public boolean readFromInputStream(InputStream in) {
        List<BaseLayoutInfo> layoutList = new ArrayList<>();
        Map<String, Object> mapAppData = (Map<String, Object>) appData;
        if (appData != null && childrenLayouts == null) {
            orderedKeySet = KeyMapping.populateOrderedKeySet(mapAppData);
            for (String key : orderedKeySet) {
                if (layout.equalsIgnoreCase(KeyMapping.GRID_LAYOUT_KEY)) {
                    LifecycleLayoutInfo lifecycleLayoutInfo = new LifecycleLayoutInfo(this);
                    lifecycleLayoutInfo.setKey(key);
                    lifecycleLayoutInfo.setAppData((Map<String, Object>) mapAppData.get(key));
                    lifecycleLayoutInfo.setFriendlyName(lifecycleLayoutInfo.getFriendlyName(key, false));
                    lifecycleLayoutInfo.setHeaderColumns(getHeaderColumns());
                    lifecycleLayoutInfo.setHeaderDesc(getHeaderDesc());
                    lifecycleLayoutInfo.setLayout(layout);
                    lifecycleLayoutInfo.splitHeaderCols();
                    lifecycleLayoutInfo.splitHeaderDesc();
                    lifecycleLayoutInfo.setKey(key);
                    layoutList.add(lifecycleLayoutInfo);
                }
            }
            setChildrenLayouts(layoutList);
            return true;
        } else if (childrenLayouts != null) {
            return true;
        }

        return false;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        Map<String, Object> mapAppData = (Map<String, Object>) appData;
        if (mapAppData != null &&
                filter != null &&
                !KeyMapping.shouldIgnoreKey(filter) &&
                mapAppData.containsKey(filter)) {
            AppLayoutInfo appLayoutInfo = new MonitorAppLayoutInfo(getParentLayout());

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
            filteredAppData.put(filter, mapAppData.get(filter));
            appLayoutInfo.appData = filteredAppData;
            appLayoutInfo.orderedKeySet = KeyMapping.populateOrderedKeySet(filteredAppData);

            return appLayoutInfo;
        }
        return this;
    }
}
