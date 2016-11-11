package com.cisco.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public class StatusRestTask {
    private Action1<Integer> loadStatusSubscriber;
    private Action1<Map<String, Object>> dataSubscriber;

    public void loadFromFile(Context context, String url) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> appData = new HashMap<>();
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open("cpe_cs_RS-441.json");
                    Scanner scanner = new Scanner(inputStream);
                    StringBuffer sb = new StringBuffer();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean loadStatus(Context context,
                              String url,
                              Action1<Integer> loadStatusSubscriber,
                              Action1<Map<String, Object>> dataSubscriber) {
        this.loadStatusSubscriber = loadStatusSubscriber;
        this.dataSubscriber = dataSubscriber;
        loadFromFile(context, url);
        return true;
    }

    private void parseJsonArray(Map<String, Object> values, String key, JSONArray jsonArray) {
        List<Object> array = null;
        if (key != null) {
            array = new ArrayList<>();
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object value = jsonArray.get(i);
                if (value instanceof JSONObject) {
                    if (key == null) {
                        parseJsonObj(values, (JSONObject) value);
                    } else {
                        array.add(parseJsonObj(values, (JSONObject) values));
                    }
                } else {
                    if (key == null) {
                        values.put(jsonArray.get(i).toString(), null);
                    } else {
                        array.add(value);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (array != null) {
            values.put(key, array);
        }
    }

    private Map<String, Object> parseJsonObj(Map<String, Object> values, JSONObject jsonObject) {
        if (values == null) {
            values = new HashMap<>();
        }
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                Object value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    values.put(key, parseJsonObj(null, (JSONObject) value));
                } else if (value instanceof JSONArray) {
                    parseJsonArray(values, key, (JSONArray) value);
                } else {
                    values.put(key, value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return values;
    }

    private void publishLoadStatus(int loadStatus) {
        Observable<Integer> observable = Observable
                .just(loadStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(loadStatusSubscriber);
    }

    private void publishStatusData(Map<String, Object> appData) {
        Observable<Map<String, Object>> observable = Observable
                .just(appData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(dataSubscriber);
    }
}
