package com.cisco.pegaserverstatusclient.background.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;

import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 10/28/16.
 */

public class ServerRefreshService extends IntentService {
    private static final String TAG = "PegaRefreshService";

    private ServerDataRestTask task;
    private SubscriberBinder binder;
    private String statusUrl;
    private boolean shouldExecute;
    private int refreshInterval;

    public ServerRefreshService() {
        super(ServerRefreshService.class.getCanonicalName());
        binder = new SubscriberBinder(new SubscriberBinder.OnForceRefresh() {
            @Override
            public void forceRefresh() {
                refreshData();
            }
        });
        task = new ServerDataRestTask();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        refreshInterval = getResources().getInteger(R.integer.data_refresh_timeout_ms);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            statusUrl = intent.getStringExtra(getString(R.string.status_url_bundle_key));
            shouldExecute = true;
        }
        while (shouldExecute) {
            try {
                Thread.sleep(refreshInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            refreshData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shouldExecute = false;
    }

    private void refreshData() {
        task.loadStatusFromNetwork(statusUrl,
                new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Log.e(TAG, "Failed to load data from URL: " + statusUrl);
                    }
                },
                new Action1<Map<String, Object>>() {
                    @Override
                    public void call(Map<String, Object> appData) {
                        binder.updateLastRefreshTime();
                        binder.subscribeObservable(initObservable(appData));
                    }
                });
    }

    private Observable initObservable(Map<String, Object> appData) {
        return Observable
                .just(appData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}