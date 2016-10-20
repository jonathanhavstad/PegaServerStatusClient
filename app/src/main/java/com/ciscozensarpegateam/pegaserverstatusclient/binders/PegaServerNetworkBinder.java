package com.ciscozensarpegateam.pegaserverstatusclient.binders;

import android.os.Binder;

import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkBinder extends Binder {
    private Map<String, Object> appData;
    private List<String> appArrayData;

    public Map<String, Object> getAppData() {
        return appData;
    }

    public void setAppData(Map<String, Object> appData) {
        this.appData = appData;
    }

    public List<String> getAppArrayData() {
        return appArrayData;
    }

    public void setAppArrayData(List<String> appArrayData) {
        this.appArrayData = appArrayData;
    }
}
