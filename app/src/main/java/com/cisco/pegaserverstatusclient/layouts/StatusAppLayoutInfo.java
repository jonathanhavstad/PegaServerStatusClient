package com.cisco.pegaserverstatusclient.layouts;

import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jonathanhavstad on 11/14/16.
 */

public class StatusAppLayoutInfo extends AppLayoutInfo {
    public StatusAppLayoutInfo() { super(null); }

    public StatusAppLayoutInfo(BaseLayoutInfo parentLayout) {
        super(parentLayout);
    }

    @Override
    public boolean readFromInputStream(InputStream in) {
        if (appData != null) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            ArrayList<BaseLayoutInfo> childrenList = new ArrayList<>();
            for (String key : mapAppData.keySet()) {
                int headerIndex = 0;
                for (String header : headerColsList) {
                    if (key.equalsIgnoreCase(header)) {
                        BasicLayoutInfo layoutInfo = new BasicLayoutInfo(this,
                                key,
                                mapAppData.get(key).toString());
                        layoutInfo.setFriendlyName(headerDescList[headerIndex]);
                        childrenList.add(layoutInfo);
                    }
                    headerIndex++;
                }
            }
            setChildrenLayouts(childrenList);
            return true;
        }
        return false;
    }

    @Override
    public BaseLayoutInfo filteredLayout(String filter) {
        return this;
    }

    @Override
    public int size() {
        return headerColsList.length;
    }
}
