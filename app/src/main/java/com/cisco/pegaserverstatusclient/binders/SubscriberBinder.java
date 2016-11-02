package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;

public class SubscriberBinder extends Binder {
    private Map<String, Object> appData;
    private long lastRefreshTime;

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

    public Map<String, Object> getAppData() {
        return appData;
    }

    public void setAppData(Map<String, Object> appData) {
        this.appData = appData;
    }

    public void updateLastRefreshTime() {
        lastRefreshTime = System.currentTimeMillis();
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }
}