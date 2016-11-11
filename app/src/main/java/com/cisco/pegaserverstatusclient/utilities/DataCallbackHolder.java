package com.cisco.pegaserverstatusclient.utilities;

import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnDataReadyListener;

import java.util.Map;

import rx.functions.Action1;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public class DataCallbackHolder {
    private BaseLayoutInfo layoutInfo;
    private int index;
    private boolean refresh;
    private Action1<Integer> dataLoadSubscriber;
    private Action1<Map<String, Object>> appDataSubscriber;
    private OnDataReadyListener onDataReadyListener;

    public DataCallbackHolder(BaseLayoutInfo layoutInfo,
                              int index,
                              boolean refresh,
                              OnDataReadyListener onDataReadyListener) {
        this.layoutInfo = layoutInfo;
        this.index = index;
        this.refresh = refresh;
        this.onDataReadyListener = onDataReadyListener;
        init();
    }

    public Action1<Integer> getDataLoadSubscriber() {
        return dataLoadSubscriber;
    }

    public Action1<Map<String, Object>> getAppDataSubscriber() {
        return appDataSubscriber;
    }

    public void setLayoutInfo(BaseLayoutInfo layoutInfo) {
        this.layoutInfo = layoutInfo;
    }

    private void init() {
        dataLoadSubscriber = new Action1<Integer>() {
            @Override
            public void call(Integer dataLoadResult) {
                if (onDataReadyListener != null) {
                    onDataReadyListener.sendDataLoadResult(DataCallbackHolder.this, dataLoadResult);
                }
            }
        };

        appDataSubscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                if (onDataReadyListener != null) {
                    onDataReadyListener.sendData(DataCallbackHolder.this, appData);
                }
            }
        };
    }

    public BaseLayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }
}
