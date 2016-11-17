package com.cisco.pegaserverstatusclient.background.tasks;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.listeners.OnDataLoadedListener;
import com.cisco.pegaserverstatusclient.rest.services.CiscoSSOWebService;
import com.cisco.pegaserverstatusclient.utilities.KeyMapping;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
 * Created by jonathanhavstad on 10/18/16.
 */

public class ServerDataRestTask {
    private static final String TAG = "PegaServerRestTask";

    public static final int AUTH_FAILURE = 0;
    public static final int AUTH_SUCCESS = 1;
    public static final int DATA_LOAD_SUCCESS = 2;
    public static final int DATA_LOAD_FAILURE = 3;
    public static final int ACCESS_TOKEN_FAILURE = -1;

    private Action1<Integer> loadStatusSubscriber;
    private Action1<Map<String, Object>> appDataSubscriber;

    private boolean loadJsonArrayFinished;
    private boolean loadJsonObjectFinished;
    private boolean loadJsonArraySuccess;
    private boolean loadJsonObjectSuccess;

    private CiscoSSOWebService ciscoSSOWebService;

    public ServerDataRestTask(Activity context) {
        ciscoSSOWebService = new CiscoSSOWebService(context);
    }

    private void loadFromFile(final Context context, final String filename, final boolean isJsonArray) {
        final AssetManager assetManager = context.getAssets();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> appData = new HashMap<>();
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open(filename);
                    Scanner scanner = new Scanner(inputStream);
                    StringBuffer sb = new StringBuffer();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                    }
                    if (isJsonArray) {
                        JSONArray jsonArray = new JSONArray(sb.toString());
                        parseJsonArray(appData, null, jsonArray);
                    } else {
                        JSONObject jsonObject = new JSONObject(sb.toString());
                        appData = parseJsonObj(appData, jsonObject);
                    }

                    loadJsonArraySuccess = true;
                    loadJsonArrayFinished = true;
                    loadJsonArraySuccess = true;
                    loadJsonArrayFinished = true;
                    publishDataLoadStatus(DATA_LOAD_SUCCESS);
                    publishAppData(appData);

                } catch (JSONException | IOException e) {
                    Log.e(TAG, e.toString());
                    loadJsonArrayFinished = true;
                    publishDataLoadStatus(DATA_LOAD_FAILURE);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }).start();
    }

    public void loadStatusFromFile(Context context, Action1<Integer> dataSubscriber) {
        this.loadStatusSubscriber = dataSubscriber;
        loadFromFile(context, context.getString(R.string.status_json_filename), true);
    }

    public boolean loadStatusFromNetwork(Context context,
                                         String url,
                                         Action1<Integer> loadStatusSubscriber,
                                         Action1<Map<String, Object>> appDataSubscriber,
                                         boolean isJsonArray) {
        boolean initResult = false;

        this.loadStatusSubscriber = loadStatusSubscriber;
        this.appDataSubscriber = appDataSubscriber;

        this.loadJsonArraySuccess = false;
        this.loadJsonObjectSuccess = false;
        this.loadJsonArrayFinished = false;
        this.loadJsonObjectFinished = false;

        if (url != null && !url.isEmpty()) {
            if (isUrlFile(url)) {
                initResult = true;
                loadFromFile(context, extractScheme(url), isJsonArray);
            } else {
                initResult = true;
                ciscoSSOWebService.loadData(url, new OnDataLoadedListener() {
                    @Override
                    public void send(JSONArray jsonArray) {
                        Map<String, Object> appData = new HashMap<>();
                        parseJsonArray(appData, null, jsonArray);
                        loadJsonArraySuccess = true;
                        loadJsonArrayFinished = true;
                        loadJsonObjectSuccess = true;
                        loadJsonObjectFinished = true;
                        publishDataLoadStatus(DATA_LOAD_SUCCESS);
                        publishAppData(appData);
                    }

                    @Override
                    public void waitForLogin() {

                    }

                    @Override
                    public void error(String error) {
                        loadJsonArrayFinished = true;
                        loadJsonObjectFinished = true;
                        publishDataLoadStatus(DATA_LOAD_FAILURE);
                    }
                });
            }
        }
        return initResult;
    }

    public static String extractBaseUrl(String url) {
        if (url != null) {
            try {
                URI uri = new URI(url);
                return uri.getScheme() + "://" + uri.getHost();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    public static String extractPathUrl(String url) {
        if (url != null) {
            try {
                URI uri = new URI(url);
                return uri.getPath();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return url;
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
                        array.add(parseJsonObj(values, (JSONObject) value));
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

    private void publishDataLoadStatus(int dataLoadStatus) {
        if (loadStatusSubscriber != null) {
            if ((loadJsonArrayFinished && loadJsonArraySuccess) ||
                    (loadJsonObjectFinished && loadJsonObjectSuccess) ||
                    (loadJsonArrayFinished && loadJsonObjectFinished)) {
                Observable<Integer> observable = Observable
                        .just(dataLoadStatus)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
                observable.subscribe(loadStatusSubscriber);
                loadJsonArraySuccess = false;
                loadJsonObjectSuccess = false;
                loadJsonArrayFinished = false;
                loadJsonObjectFinished = false;
            }
        }
    }

    private void publishAppData(Map<String, Object> appData) {
        Observable<Map<String, Object>> observable = Observable
                .just(appData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(appDataSubscriber);
    }

    private boolean isUrlFile(String url) {
        if (url.contains(KeyMapping.FILE_URI_KEY)) {
            return true;
        }
        return false;
    }

    private String extractScheme(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                URI uri = new URI(url);
                String scheme = uri.getScheme() + "://";
                int schemeIndex = url.indexOf(scheme);
                return url.substring(schemeIndex + scheme.length());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }
        return url;
    }
}
