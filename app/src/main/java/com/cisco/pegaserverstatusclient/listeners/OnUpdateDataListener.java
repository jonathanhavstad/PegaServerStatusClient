package com.cisco.pegaserverstatusclient.listeners;

import com.cisco.pegaserverstatusclient.binders.ServerDataBinder;

/**
 * Created by jonathanhavstad on 10/31/16.
 */

public interface OnUpdateDataListener {
    void setPageTitle(String title);
    void initPegaDataActivity(String friendlyName, ServerDataBinder childBinder);
    void requestRefresh();
}
