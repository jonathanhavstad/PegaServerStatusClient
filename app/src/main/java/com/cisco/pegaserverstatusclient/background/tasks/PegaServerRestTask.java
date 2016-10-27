package com.cisco.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.ServerLayoutInfo;
import com.cisco.pegaserverstatusclient.rest.services.IBPMStatusService;
import com.cisco.pegaserverstatusclient.rest.services.OauthAccessService;
import com.cisco.pegaserverstatusclient.rest.services.OauthRedirectService;
import com.google.gson.Gson;

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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

/**
 * Created by jonathanhavstad on 10/18/16.
 */

public class PegaServerRestTask {
    public static final int AUTH_FAILURE = 0;
    public static final int AUTH_SUCCESS = 1;
    public static final int DATA_LOAD_SUCCESS = 2;
    public static final int DATA_LOAD_FAILURE = 3;
    public static final int ACCESS_TOKEN_FAILURE = -1;

    private static final String TAG = "PegaServerRestTask";

    private static final String CSRF_TOKEN_KEY = "csrftoken=";

    private static final int AUTH_FAILURE_CODE = 403;
    private static final int AUTH_TRY_AGAIN_CODE = 200;
    private Map<String, Object> appData;
    private boolean authSuccessful;
    private boolean haveAccessToken;
    private ConnectableObservable<Integer> authObservable;
    private Action1<Integer> authSubscriber;
    private Action1<Integer> dataSubscriber;
    private Action1<BaseLayoutInfo> layoutSubscriber;
    private Context context;

    public PegaServerRestTask(Context context, Map<String, Object> appData) {
        this.appData = appData;
        this.context = context;
    }

    private void createAuthObservable() {
        this.authObservable = Observable
                .just(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .publish();
    }

    private void loadFromFile(final Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();

        new Thread(new Runnable() {
            @Override
            public void run() {
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
                    sendAuthStatus(DATA_LOAD_SUCCESS);
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

    private void loadRedirectUrl(final String redirectUrl,
                                final String username,
                                final String password,
                                final String cookies,
                                final boolean post) {
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().followRedirects(false);
        OkHttpClient client = clientBuilder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl("https://www.instagram.com")
                .build();
        OauthAccessService oauthAccessService = retrofit.create(OauthAccessService.class);

        if (cookies != null && !cookies.isEmpty() && post) {
            String csrfToken = extractCsrfToken(cookies);

            if (csrfToken != null) {

                oauthAccessService.authorizeUser(
                        redirectUrl,
                        username,
                        password,
                        csrfToken, cookies)
                        .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(
                            Call<ResponseBody> call,
                            Response<ResponseBody> response) {
                        Log.d(TAG, "Request URL: " + response.raw().request().url());
                        Log.d(TAG, "Request headers: " + response.raw().headers().toString());
                        Log.d(TAG, "Response code: " + response.code());
                        String cookies = concatCookies(response.headers().values("set-cookie"));
                        authSuccessful =
                                (response.code() != AUTH_FAILURE_CODE &&
                                response.code() != AUTH_TRY_AGAIN_CODE);
                        loadRedirectUrl(
                                response.raw().headers().get("location"),
                                username,
                                password,
                                cookies,
                                false);
                        subscribeToAuthObservable();
                        authObservable.connect();
                        if (!authSuccessful) {
                            sendAuthStatus(AUTH_FAILURE);
                        } else {
                            sendAuthStatus(AUTH_SUCCESS);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "Failure: " + t.toString());
                    }
                });
                return;
            }
        }
        if (redirectUrl != null) {
            oauthAccessService
                    .redirect(redirectUrl, cookies).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d(TAG, "Request URL: " + response.raw().request().url());
                    Log.d(TAG, "Request headers: " + response.raw().headers().toString());
                    Log.d(TAG, "Response code: " + response.code());
                    String cookies = concatCookies(response.headers().values("set-cookie"));
                    if (!(haveAccessToken = hasAccessToken(response))) {
                        loadRedirectUrl(
                                response.raw().headers().get("location"),
                                username,
                                password,
                                cookies,
                                post);
                    } else {
                        Log.d(TAG, "Access token: " + extractAccessToken(response));
                    }
                    subscribeToAuthObservable();
                    authObservable.connect();
                    if (authSuccessful && !haveAccessToken) {
                        sendAuthStatus(ACCESS_TOKEN_FAILURE);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Failure: " + t.toString());
                }
            });
        }
    }

    private boolean hasLocation(Response<ResponseBody> response) {
        return (response.raw().headers().get("location") != null);
    }

    private boolean hasAccessToken(Response<ResponseBody> response) {
        return hasLocation(response)
                && response.raw().headers().get("location").contains("access_token=");
    }

    private String extractAccessToken(Response<ResponseBody> response) {
        String accessToken = null;
        if (hasLocation(response)) {
            String location = response.raw().headers().get("location");
            int accessTokenStartIndex = location.indexOf("access_token=");
            if (accessTokenStartIndex >= 0) {
                accessTokenStartIndex += "access_token=".length() + 1;
                accessToken = location.substring(accessTokenStartIndex);
            }
        }
        return accessToken;
    }

    private String concatCookies(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(value);
            sb.append("; ");
        }
        return sb.toString();
    }

    private void retrieveAccessToken(final String authUrl) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().followRedirects(false);
        OkHttpClient client = clientBuilder.build();
        Retrofit retrofit =
                new Retrofit.Builder().client(client).baseUrl(authUrl).build();
        OauthRedirectService oauthRedirectService = retrofit.create(OauthRedirectService.class);
        oauthRedirectService.getAccessToken().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    private void retrieveAccessToken(final String authUrl,
                                    final String username,
                                    final String password) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().followRedirects(false);
        OkHttpClient client = clientBuilder.build();
        Retrofit retrofit =
                new Retrofit.Builder().client(client).baseUrl(authUrl).build();
        OauthRedirectService oauthRedirectService = retrofit.create(OauthRedirectService.class);
        oauthRedirectService.getAccessToken().enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "Request URL: " + response.raw().request().url().toString());
                Log.d(TAG, "Response code: " + response.code());
                String cookies = concatCookies(response.headers().values("set-cookie"));
                loadRedirectUrl(response.raw().headers().get("location"),
                        username,
                        password,
                        cookies,
                        true);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Failure: " + t.toString());
            }
        });
    }

    private String extractCsrfToken(String cookieValue) {
        int startIndex = cookieValue.indexOf(CSRF_TOKEN_KEY);
        if (startIndex >= 0) {
            startIndex += CSRF_TOKEN_KEY.length();
            int endIndex = cookieValue.indexOf(';', startIndex);
            return cookieValue.substring(startIndex, endIndex);
        }
        return null;
    }

    public void loadStatusFromFile(Action1<Integer> dataSubscriber) {

        this.dataSubscriber = dataSubscriber;
        loadFromFile(context, context.getString(R.string.status_json_filename));
    }

    public void loadStatusFromNetwork(String url, Action1<Integer> dataSubscriber) {
        this.dataSubscriber = dataSubscriber;
        if (url != null) {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(extractBaseUrl(url)).build();
            IBPMStatusService ibpmStatusService = retrofit.create(IBPMStatusService.class);
            ibpmStatusService.getStatus(extractPathUrl(url)).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    JSONArray jsonArray = null;
                    try {
                        jsonArray = new JSONArray(response.body().string());
                        parseJsonArray(appData, null, jsonArray);
                        sendAuthStatus(DATA_LOAD_SUCCESS);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        sendAuthStatus(DATA_LOAD_FAILURE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendAuthStatus(DATA_LOAD_FAILURE);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    sendAuthStatus(DATA_LOAD_FAILURE);
                }
            });
        }
    }

    private String extractBaseUrl(String url) {
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

    private String extractPathUrl(String url) {
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

    private void subscribeToAuthObservable() {
        authObservable.subscribe(authSubscriber);
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

    private void sendAuthStatus(int authStatus) {
        if (dataSubscriber != null) {
            Observable<Integer> observable = Observable
                    .just(authStatus)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
            observable.subscribe(dataSubscriber);
        }
    }
    public void loadServerInfo(Context context,
                               String url,
                               Action1<BaseLayoutInfo> layoutSubscriber) {
        this.layoutSubscriber = layoutSubscriber;
        loadServerLayoutFromFile(context, url);
    }

    private void loadServerLayoutFromFile(Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open(filename);
                    sendServerLayout(parseServerInfo(readInputStream(inputStream)));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void sendServerLayout(ServerLayoutInfo serverInfo) {
        Observable<ServerLayoutInfo> observable = Observable
                .just(serverInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(layoutSubscriber);
    }

    private ServerLayoutInfo parseServerInfo(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ServerLayoutInfo.class);
    }

    public void loadAppInfo(Context context,
                            String url,
                            Action1<BaseLayoutInfo> layoutSubscriber) {
        this.layoutSubscriber = layoutSubscriber;
        loadAppLayoutFromFile(context, url);
    }

    private void loadAppLayoutFromFile(Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open(filename);
                    sendAppLayout(parseAppInfo(readInputStream(inputStream)));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void sendAppLayout(AppLayoutInfo appLayoutInfo) {
        Observable<AppLayoutInfo> observable = Observable
                .just(appLayoutInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(layoutSubscriber);
    }

    private AppLayoutInfo parseAppInfo(String json) {
        Gson gson = new Gson();
        AppLayoutInfo appLayoutInfo = gson.fromJson(json, AppLayoutInfo.class);
        return appLayoutInfo;
    }

    public void loadDomainInfo(Context context,
                                     String url,
                                     Action1<BaseLayoutInfo> layoutSubscriber) {
        this.layoutSubscriber = layoutSubscriber;
        loadDomainLayoutFromFile(context, url);
    }

    private void loadDomainLayoutFromFile(Context context, final String filename) {
        final AssetManager assetManager = context.getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open(filename);
                    sendDomainLayout(parseDomainInfo(readInputStream(inputStream)));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    private void sendDomainLayout(DomainLayoutInfo domainInfo) {
        Observable<DomainLayoutInfo> observable = Observable
                .just(domainInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(layoutSubscriber);
    }

    private DomainLayoutInfo parseDomainInfo(String json) {
        Gson gson = new Gson();
        DomainLayoutInfo domainInfo = gson.fromJson(json, DomainLayoutInfo.class);
        return domainInfo;
    }

    private String readInputStream(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream);
        StringBuffer sb = new StringBuffer();
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
        }
        scanner.close();
        return sb.toString();
    }
}
