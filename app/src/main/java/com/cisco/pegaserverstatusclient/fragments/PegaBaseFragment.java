package com.cisco.pegaserverstatusclient.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;

import icepick.Icepick;
import icepick.State;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class PegaBaseFragment extends Fragment {
    @State
    String friendlyName;

    public String getFriendlyName() {
        return friendlyName;
    }

    public interface OnSendDataListener {
        void setPageTitle(String title);
        void sendData(String friendlyName, PegaServerNetworkBinder childBinder);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
    }
}
