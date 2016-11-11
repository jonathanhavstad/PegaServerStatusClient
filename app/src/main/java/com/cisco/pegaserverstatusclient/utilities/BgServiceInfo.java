package com.cisco.pegaserverstatusclient.utilities;

import android.content.ServiceConnection;

import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnBgDataReadyListener;

import java.util.Map;

import rx.functions.Action1;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public class BgServiceInfo {
    private ServiceConnection connection;
    private Action1<Map<String, Object>> subscriber;
    private SubscriberBinder binder;
    private boolean bgServiceStarted;
    private int index;
    private BaseLayoutInfo layoutInfo;
    private OnBgDataReadyListener onBgDataReadyListener;

    public BgServiceInfo(int index,
                         BaseLayoutInfo layoutInfo,
                         OnBgDataReadyListener onBgDataReadyListener) {
        this.index = index;
        this.layoutInfo = layoutInfo;
        this.onBgDataReadyListener = onBgDataReadyListener;
        initSubscriber();
    }

    private void initSubscriber() {
        subscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                if (onBgDataReadyListener != null) {
                    onBgDataReadyListener.send(BgServiceInfo.this, appData);
                }
            }
        };
    }

    public ServiceConnection getConnection() {
        return connection;
    }

    public void setConnection(ServiceConnection connection) {
        this.connection = connection;
    }

    public Action1<Map<String, Object>> getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Action1<Map<String, Object>> subscriber) {
        this.subscriber = subscriber;
    }

    public SubscriberBinder getBinder() {
        return binder;
    }

    public void setBinder(SubscriberBinder binder) {
        this.binder = binder;
    }

    public boolean isBgServiceStarted() {
        return bgServiceStarted;
    }

    public void setBgServiceStarted(boolean bgServiceStarted) {
        this.bgServiceStarted = bgServiceStarted;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public BaseLayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public void setLayoutInfo(BaseLayoutInfo layoutInfo) {
        this.layoutInfo = layoutInfo;
    }
}
