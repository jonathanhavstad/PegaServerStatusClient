package com.ciscozensarpegateam.pegaserverstatusclient.background.tasks;

import android.content.Context;
import android.util.Log;

import com.ciscozensarpegateam.pegaserverstatusclient.R;
import com.ciscozensarpegateam.pegaserverstatusclient.rest.services.OauthAccessService;
import com.ciscozensarpegateam.pegaserverstatusclient.rest.services.OauthRedirectService;

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
    private static final String TAG = "PegaServerRestTask";
    private static final String CSRF_TOKEN_KEY = "csrftoken=";
    private static final int AUTH_FAILURE_CODE = 403;
    private static final int AUTH_TRY_AGAIN_CODE = 200;
    private Map<String, Object> appData;
    private boolean authSuccessful;
    private boolean haveAccessToken;
    private ConnectableObservable<Integer> authObservable;
    private Action1<Integer> authSubscriber;
    private Action1<Boolean> dataSubscriber;
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
        new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = context.getAssets().open(filename);

                    Scanner scanner = new Scanner(inputStream);
                    StringBuffer sb = new StringBuffer();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                    }
                    JSONArray jsonArray = new JSONArray(sb.toString());
                    parseJsonArray(appData, null, jsonArray);
                    sendAuthStatus(true);
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                } catch (IOException e) {
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
        }
                .run();
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
                            sendAuthStatus(false);
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
                        sendAuthStatus(false);
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

    public void loadFromNetwork(String restUrl,
                                String authUrl,
                                String username,
                                String password,
                                Action1<Boolean> dataSubscriber) {
        this.dataSubscriber = dataSubscriber;
        createAuthObservable();
        createAuthSubscriber();
        retrieveAccessToken(authUrl, username, password);
    }

    private void createAuthSubscriber() {
        authSubscriber = new Action1<Integer>() {
            @Override
            public void call(Integer value) {
                Log.d(TAG, "Authorization state change observed");
                if (authSuccessful && haveAccessToken) {
                    Log.d(TAG, "Loading Pega Server JSON data");
                    loadFromFile(context, context.getString(R.string.status_json_filename));
                }
            }
        };
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

    private void sendAuthStatus(boolean authStatus) {
        if (dataSubscriber != null) {
            Observable<Boolean> observable = Observable
                    .just(authStatus)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
            observable.subscribe(dataSubscriber);
        }
    }
}
