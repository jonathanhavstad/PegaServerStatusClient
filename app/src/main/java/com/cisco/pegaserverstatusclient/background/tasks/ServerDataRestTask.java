package com.cisco.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.DomainAppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.ServerLayoutInfo;
import com.cisco.pegaserverstatusclient.rest.services.IBPMStatusService;
import com.cisco.pegaserverstatusclient.rest.services.OauthAccessService;
import com.cisco.pegaserverstatusclient.rest.services.OauthRedirectService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
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

    private void loadFromFile(final Context context, final String filename) {
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
                    JSONArray jsonArray = new JSONArray(sb.toString());
                    parseJsonArray(appData, null, jsonArray);
                    publishDataLoadStatus(DATA_LOAD_SUCCESS);
                } catch (JSONException | IOException e) {
                    Log.e(TAG, e.toString());
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
        loadFromFile(context, context.getString(R.string.status_json_filename));
    }

    public void loadStatusFromNetwork(String url,
                                      Action1<Integer> loadStatusSubscriber,
                                      Action1<Map<String, Object>> appDataSubscriber) {
        this.loadStatusSubscriber = loadStatusSubscriber;
        this.appDataSubscriber = appDataSubscriber;

        this.loadJsonArraySuccess = false;
        this.loadJsonObjectSuccess = false;
        this.loadJsonArrayFinished = false;
        this.loadJsonObjectFinished = false;

        if (url != null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(extractBaseUrl(url))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            IBPMStatusService ibpmStatusService = retrofit.create(IBPMStatusService.class);
            ibpmStatusService
                    .getStatusWithJsonArray(extractPathUrl(url))
                    .enqueue(new Callback<JsonArray>() {
                        @Override
                        public void onResponse(Call<JsonArray> call,
                                               Response<JsonArray> response) {
                            try {
                                if (response.body() != null) {
                                    Map<String, Object> appData = new HashMap<>();
                                    JSONArray jsonArray = new JSONArray(response.body().toString());
                                    parseJsonArray(appData, null, jsonArray);
                                    loadJsonArraySuccess = true;
                                    loadJsonArrayFinished = true;
                                    publishDataLoadStatus(DATA_LOAD_SUCCESS);
                                    publishAppData(appData);
                                } else {
                                    Log.e(TAG, "Response body was null!");
                                    loadJsonArrayFinished = true;
                                    publishDataLoadStatus(DATA_LOAD_FAILURE);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                loadJsonArrayFinished = true;
                                publishDataLoadStatus(DATA_LOAD_FAILURE);
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonArray> call, Throwable t) {
                            Log.e(TAG, "Network failure: " + t.toString());
                            loadJsonArrayFinished = true;
                            publishDataLoadStatus(DATA_LOAD_FAILURE);
                        }
                    });
            ibpmStatusService
                    .getStatusWithJsonObject(extractPathUrl(url))
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            try {
                                if (response.body() != null) {
                                    Map<String, Object> appData = new HashMap<>();
                                    JSONObject jsonObject = new JSONObject(response.body().toString());
                                    parseJsonObj(appData, jsonObject);
                                    loadJsonObjectSuccess = true;
                                    loadJsonObjectFinished = true;
                                    publishDataLoadStatus(DATA_LOAD_SUCCESS);
                                    publishAppData(appData);
                                } else {
                                    Log.e(TAG, "Response body was null!");
                                    loadJsonObjectFinished = true;
                                    publishDataLoadStatus(DATA_LOAD_FAILURE);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                loadJsonObjectFinished = true;
                                publishDataLoadStatus(DATA_LOAD_FAILURE);
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Log.e(TAG, "Network failure: " + t.toString());
                            loadJsonObjectFinished = true;
                            publishDataLoadStatus(DATA_LOAD_FAILURE);
                        }
                    });
        }
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
}
