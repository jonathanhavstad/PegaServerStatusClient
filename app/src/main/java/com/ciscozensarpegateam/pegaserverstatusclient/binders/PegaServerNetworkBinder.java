package com.ciscozensarpegateam.pegaserverstatusclient.binders;

import android.os.Binder;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkBinder extends Binder {
    private Object appData;

    public Object getAppData() {
        return appData;
    }

    public void setAppData(Object appData) {
        this.appData = appData;
    }
}
