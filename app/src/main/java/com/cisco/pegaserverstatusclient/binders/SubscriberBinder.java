package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;

public class SubscriberBinder extends Binder {
    private long lastRefreshTime;
    private BaseLayoutInfo layoutInfo;

    public interface OnForceRefresh {
        void forceRefresh();
    }

    private OnForceRefresh onForceRefresh;
    private List<Action1> subscribers = new ArrayList<>();

    public SubscriberBinder(OnForceRefresh onForceRefresh) {
        this.onForceRefresh = onForceRefresh;
    }

    public void addSubscriber(Action1<Map<String, Object>> subscriber) {
        subscribers.add(subscriber);
    }

    public void removeSubscriber(Action1<Map<String, Object>> subscriber) {
        subscribers.remove(subscriber);
    }

    public void subscribeObservable(Observable observable) {
        for (Action1 subscriber : subscribers) {
            observable.subscribe(subscriber);
        }
    }

    public void forceRefresh() {
        if (this.onForceRefresh != null) {
            onForceRefresh.forceRefresh();
        }
    }

    public void updateLastRefreshTime() {
        lastRefreshTime = System.currentTimeMillis();
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public BaseLayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public void setLayoutInfo(BaseLayoutInfo layoutInfo) {
        this.layoutInfo = layoutInfo;
    }
}