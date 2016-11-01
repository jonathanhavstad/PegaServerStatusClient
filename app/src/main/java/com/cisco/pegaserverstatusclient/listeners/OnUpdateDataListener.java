package com.cisco.pegaserverstatusclient.listeners;

import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;

/**
 * Created by jonathanhavstad on 10/31/16.
 */

public interface OnUpdateDataListener {
    void setPageTitle(String title);
    void sendData(String friendlyName, PegaServerNetworkBinder childBinder);
    void requestRefresh();
}
