package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class BaseLayoutInfoBinder extends Binder {
    private BaseLayoutInfo baseLayoutInfo;

    public BaseLayoutInfo getBaseLayoutInfo() {
        return baseLayoutInfo;
    }

    public void setBaseLayoutInfo(BaseLayoutInfo baseLayoutInfo) {
        this.baseLayoutInfo = baseLayoutInfo;
    }
}
