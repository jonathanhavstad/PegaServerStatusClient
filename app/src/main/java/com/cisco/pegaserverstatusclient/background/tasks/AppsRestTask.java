package com.cisco.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.content.res.AssetManager;

import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class AppsRestTask {
    public static final int LOAD_SUCCESS = 1;
    public static final int LOAD_FAILURE = 0;
    public static final int LOAD_NOT_STARTED = -1;

    private Action1<Integer> loadStatusSubscriber;
    private Action1<List<AppLayoutInfo>> appsSubscriber;

    public void loadAppsLayout(Context context,
                               String appsUrl,
                               Action1<Integer> loadStatusSubscriber,
                               Action1<List<AppLayoutInfo>> appsSubscriber) {
        this.loadStatusSubscriber = loadStatusSubscriber;
        this.appsSubscriber = appsSubscriber;
        loadFromFile(context, "apps.json");
    }

    private void loadFromFile(Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AppLayoutInfo> appLayoutInfoList = new ArrayList<>();
                InputStream in = null;
                try {
                    in = assetManager.open(filename);
                    Gson gson = new Gson();
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(in));
                    JsonArray jsonArray = gson.fromJson(jsonReader, JsonArray.class);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        AppLayoutInfo appLayoutInfo =
                                gson.fromJson(jsonArray.get(i), AppLayoutInfo.class);
                        appLayoutInfo.splitHeaderCols();
                        appLayoutInfo.splitHeaderDesc();
                        appLayoutInfoList.add(appLayoutInfo);
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
                    publishAppsLoadStatus(LOAD_SUCCESS);
                    publishAppsLayout(appLayoutInfoList);
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

    private void publishAppsLayout(List<AppLayoutInfo> appLayoutInfoList) {
        Observable<List<AppLayoutInfo>> observable = Observable
                .just(appLayoutInfoList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(appsSubscriber);
    }
}
