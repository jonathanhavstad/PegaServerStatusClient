package com.cisco.pegaserverstatusclient.data;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class AppLayoutInfo extends BaseLayoutInfo {
    public static final String APP_JSON_KEY = "APPS";

    private String friendlyName;

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
}
