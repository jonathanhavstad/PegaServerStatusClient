package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import java.util.ArrayList;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkBinder<T> extends Binder {
    private T appData;
    private String parentKey;
    private ArrayList<String> keyPath;
    private T drawerData;

    public T getAppData() {
        return appData;
    }

    public void setAppData(T appData) {
        this.appData = appData;
    }

    public ArrayList<String> getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(ArrayList<String> keyPath) {
        this.keyPath = keyPath;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public T getDrawerData() {
        return drawerData;
    }

    public void setDrawerData(T drawerData) {
        this.drawerData = drawerData;
    }
}
