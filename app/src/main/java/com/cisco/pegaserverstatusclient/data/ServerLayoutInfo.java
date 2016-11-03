package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaChildFragment;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class ServerLayoutInfo extends BaseLayoutInfo {
    public static final String SERVER_JSON_KEY = "HOSTS";

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public BaseLayoutInfo createChildLayout(String parentKey) {
        return null;
    }

    @Override
    public PegaBaseFragment addLayoutToView(Context context,
                                            String parentKey,
                                            ArrayList<String> keyPath,
                                            Object appData,
                                            AddLayoutViewAdapter addLayoutViewAdapter) {
        PegaBaseFragment fragment = null;

        if (addLayoutViewAdapter != null) {
            fragment =
                    PegaChildFragment
                            .newInstance(context,
                                    friendlyName,
                                    parentKey,
                                    key,
                                    (ArrayList<String>) keyPath.clone(),
                                    appData);
            addLayoutViewAdapter.add(fragment);
        }

        return fragment;
    }

    @Override
    public PegaBaseFragment replaceLayoutToView(Context context,
                                                String parentKey,
                                                ArrayList<String> keyPath,
                                                Object appData,
                                                ReplaceLayoutViewAdapter replaceLayoutViewAdapter) {
        PegaBaseFragment fragment = null;

        if (replaceLayoutViewAdapter != null) {
            fragment =
                    PegaChildFragment
                            .newInstance(context,
                                    friendlyName,
                                    parentKey,
                                    key,
                                    (ArrayList<String>) keyPath.clone(),
                                    appData);
            replaceLayoutViewAdapter.replace(true, fragment);
        }

        return fragment;
    }

    @Override
    public Object getValue(Map<String, Object> appData, String childValue) {
        if (appData != null) {
            return appData.get(key);
        }
        return null;
    }
}
