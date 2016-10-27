package com.cisco.pegaserverstatusclient.data;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class ServerLayoutInfo extends BaseLayoutInfo {
    public static final String SERVER_JSON_KEY = "HOSTS";

    private String friendlyName;

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
}
