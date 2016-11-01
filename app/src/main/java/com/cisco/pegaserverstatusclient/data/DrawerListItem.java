package com.cisco.pegaserverstatusclient.data;

/**
 * Created by jonathanhavstad on 10/31/16.
 */

public class DrawerListItem {
    private String friendlyName;
    private String key;

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
    public String toString() {
        return friendlyName;
    }
}
