package com.cisco.pegaserverstatusclient.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import icepick.Icepick;
import icepick.State;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class PegaBaseFragment<T> extends Fragment {
    @State
    String friendlyName;

    @State
    ArrayList<String> keyPath;

    protected T appData;

    public String getFriendlyName() {
        return friendlyName;
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

    public Object getAppData() {
        return appData;
    }

    public String getKey() {
        if (keyPath != null && keyPath.size() > 0) {
            return keyPath.get(keyPath.size() - 1);
        }
        return null;
    }

    public abstract boolean notifyAppDataChanged(Object appData);
}
