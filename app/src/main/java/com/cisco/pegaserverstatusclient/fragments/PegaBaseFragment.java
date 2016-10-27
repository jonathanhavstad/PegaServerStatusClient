package com.cisco.pegaserverstatusclient.fragments;

import android.support.v4.app.Fragment;

import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class PegaBaseFragment extends Fragment {
    protected String friendlyName;

    public String getFriendlyName() {
        return friendlyName;
    }

    public interface OnSendDataListener {
        void setCurrentPageTitle();
        void setPageTitle(String title);
        void sendData(String friendlyName, PegaServerNetworkBinder childBinder);
    }
}
