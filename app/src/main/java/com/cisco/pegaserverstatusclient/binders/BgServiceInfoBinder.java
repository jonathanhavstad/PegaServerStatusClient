package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import com.cisco.pegaserverstatusclient.utilities.BgServiceInfo;

/**
 * Created by jonathanhavstad on 11/16/16.
 */

public class BgServiceInfoBinder extends Binder {
    private BgServiceInfo[] bgServiceInfoList;

    public BgServiceInfo[] getBgServiceInfoList() {
        return bgServiceInfoList;
    }

    public void setBgServiceInfoList(BgServiceInfo[] bgServiceInfoList) {
        this.bgServiceInfoList = bgServiceInfoList;
    }
}
