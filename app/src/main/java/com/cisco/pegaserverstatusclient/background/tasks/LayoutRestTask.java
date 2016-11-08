package com.cisco.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.content.res.AssetManager;

import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;

import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class LayoutRestTask {
    public static final int LOAD_SUCCESS = 1;
    public static final int LOAD_FAILURE = 0;
    public static final int LOAD_NOT_STARTED = -1;

    private Action1<Integer> loadStatusSubscriber;
    private Action1<BaseLayoutInfo> layoutSubscriber;
    private BaseLayoutInfo layoutInfo;

    public void loadAppsLayout(Context context,
                               BaseLayoutInfo layoutInfo,
                               Action1<Integer> loadStatusSubscriber,
                               Action1<BaseLayoutInfo> layoutSubscriber) {
        this.layoutInfo = layoutInfo;
        this.loadStatusSubscriber = loadStatusSubscriber;
        this.layoutSubscriber = layoutSubscriber;
        loadFromFile(context, "apps.json");
    }

    private void loadFromFile(Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                try {
                    in = assetManager.open(filename);
                    if (layoutInfo.readFromNetwork(in)) {
                        publishAppsLoadStatus(LOAD_SUCCESS);
                        publishLayout(layoutInfo);
                    } else {
                        publishAppsLoadStatus(LOAD_FAILURE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    publishAppsLoadStatus(LOAD_FAILURE);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void publishAppsLoadStatus(int appsLoadStatus) {
        Observable<Integer> observable = Observable
                .just(appsLoadStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(loadStatusSubscriber);
    }

    private void publishLayout(BaseLayoutInfo layoutInfo) {
        Observable<BaseLayoutInfo> observable = Observable
                .just(layoutInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(layoutSubscriber);
    }
}
