package com.cisco.pegaserverstatusclient.background.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;

import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 10/28/16.
 */

public class PegaServerRefreshService extends IntentService {
    private static final String TAG = "PegaRefreshService";

    private PegaServerRestTask task;
    private SubscriberBinder binder;
    private boolean shouldExecute;
    private Map<AppLayoutInfo, Map<String, Object>> appsData;
    private int refreshInterval;

    public PegaServerRefreshService() {
        super(PegaServerRefreshService.class.getCanonicalName());
        binder = new SubscriberBinder(new SubscriberBinder.OnForceRefresh() {
            @Override
            public void forceRefresh() {
                refreshAppsData();
            }
        });
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
        shouldExecute = true;
        while (shouldExecute) {
            try {
                Thread.sleep(refreshInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            refreshAppsData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shouldExecute = false;
    }

    private void refreshAppsData() {
        if (appsData == null) {
            appsData = binder.getAppsData();
        }
        for (AppLayoutInfo appLayoutInfo : appsData.keySet()) {
            refreshData(appLayoutInfo.getUrl(), appsData.get(appLayoutInfo));
        }
    }

    private void refreshData(final String statusUrl, Map<String, Object> appData) {
        if (appData != null) {
            if (task == null) {
                task = new PegaServerRestTask();
            }
            task.loadStatusFromNetwork(statusUrl,
                    new Action1<Integer>() {
                        @Override
                        public void call(Integer loadStatus) {
                            if (loadStatus == PegaServerRestTask.DATA_LOAD_SUCCESS) {
                                binder.updateLastRefreshTime();
                            } else if (loadStatus == PegaServerRestTask.DATA_LOAD_FAILURE) {
                                Log.e(TAG, "Failed to load data from URL: " + statusUrl + "!");
                            }
                        }
                    },
                    new Action1<Map<String, Object>>() {
                        @Override
                        public void call(Map<String, Object> appData) {

                        }
                    });
        }
    }

    private Observable initObservable(Map<String, Object> appData) {
        return Observable
                .just(appData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
