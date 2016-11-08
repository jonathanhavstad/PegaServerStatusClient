package com.cisco.pegaserverstatusclient.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cisco.pegaserverstatusclient.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.cisco.pegaserverstatusclient.background.services.RegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.rest.services.IBPMStatusService;
import com.cisco.pegaserverstatusclient.views.CiscoLoginFormWebView;
import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Set;

import io.fabric.sdk.android.Fabric;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private static final int ACCESS_DATA_REQUEST_CODE = 1000;

    private String LOGOUT_URL;
    private String HOME_PAGE;
    private String LOGIN_URL;
    private String SSO_LOGIN_URL;
    private String LOGOUT_COMPLETE_URL;
    private String SSO_COOKIE_KEY;
    private boolean beginLogin;
    private boolean beginAuthentication;
    private boolean loadUrlFromIntent;
    private String statusUrl;

    @BindView(R.id.login_web_view)
    CiscoLoginFormWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        loadWebValues();

        enableSpecificWebSettings();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Loading URL: " + url);
                Log.d(TAG, "Site cookie: " + CookieManager.getInstance().getCookie(url));
                Log.d(TAG, "SSO Cookie: " + getSSOCookie(CookieManager.getInstance().getCookie(url)));
                String ssoCookie = getSSOCookie(CookieManager.getInstance().getCookie(url));

                CookieManager.getInstance().setAcceptCookie(true);

                String newUrl = null;
                if (url.equals(LOGOUT_COMPLETE_URL)) {
                    newUrl = HOME_PAGE;
                } else if (url.equals(HOME_PAGE)) {
                    newUrl = LOGIN_URL;
                } else if (url.equals(SSO_LOGIN_URL)) {
                    beginLogin = true;
                    beginAuthentication = false;
                    view.setVisibility(View.VISIBLE);
                } else if (beginLogin) {
                    beginAuthentication = true;
                    final String statusUrl = url;
                    String baseUrl = ServerDataRestTask.extractBaseUrl(url);
                    String pathUrl = ServerDataRestTask.extractPathUrl(url);
                    Retrofit retrofit = new Retrofit
                            .Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    IBPMStatusService service = retrofit.create(IBPMStatusService.class);
                    service.getStatusWithJsonArray(pathUrl).enqueue(new Callback<JsonArray>() {
                        @Override
                        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                            Log.d(TAG, "Received response");
                            if (response != null &&
                                    response.body() != null &&
                                    response.body().isJsonArray()) {
                                launchMainActivity(statusUrl);
                                beginLogin = false;
                                beginAuthentication = false;
                            } else {
                                Log.e(TAG, "Response body is not acceptable!");
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonArray> call, Throwable t) {
                            Log.e(TAG, "Received JSON error: " + t.toString());
                        }
                    });
                    service.getStatusWithJsonObject(pathUrl).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            Log.d(TAG, "Received response");
                            if (response != null &&
                                    response.body() != null &&
                                    (response.body().isJsonObject())) {
                                launchMainActivity(statusUrl);
                                beginLogin = false;
                                beginAuthentication = false;
                            } else {
                                Log.e(TAG, "Response body is not acceptable!");
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Log.e(TAG, "Received JSON error: " + t.toString());
                        }
                    });
                }

                if (newUrl != null) {
                    Log.d(TAG, "Redirecting to: " + newUrl);
                    view.loadUrl(newUrl);
                } else {
                    super.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "URL finished loading: " + url);
                CookieManager.getInstance().setAcceptCookie(true);
            }
        });

        readIntent(getIntent());

        beginLogin = false;
        beginAuthentication = false;

        webView.setVisibility(View.INVISIBLE);
        webView.loadUrl(LOGOUT_URL);

        Intent serviceIntent = new Intent(this, RegistrationIntentService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCESS_DATA_REQUEST_CODE) {
            beginLogin = false;
            beginAuthentication = false;
            webView.clearCache(true);
            webView.setVisibility(View.INVISIBLE);
            webView.loadUrl(LOGOUT_URL);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadWebValues() {
        LOGOUT_URL = getString(R.string.cisco_logout_url);
        HOME_PAGE = getString(R.string.cisco_home_page_url);
        LOGIN_URL = getString(R.string.cisco_login_url);
        SSO_LOGIN_URL = getString(R.string.cisco_sso_login_url);
        LOGOUT_COMPLETE_URL = getString(R.string.cisco_logout_complete_url);
        SSO_COOKIE_KEY = getString(R.string.sso_cookie_key);
    }

    private void enableSpecificWebSettings() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.clearCache(true);

        CookieManager.getInstance().setAcceptCookie(true);
    }

    private void launchMainActivity(String url) {
        Intent intent = new Intent(this, ServerAppsActivity.class);
        intent.putExtra(getString(R.string.status_url_bundle_key), url);
        startActivityForResult(intent, ACCESS_DATA_REQUEST_CODE);
    }

    private String getSSOCookie(String cookie) {
        if (cookie != null) {
            int startIndex = cookie.indexOf(SSO_COOKIE_KEY) + SSO_COOKIE_KEY.length() + 1;
            int endIndex = cookie.length();
            if (startIndex >= 0) {
                int tempEndIndex = cookie.indexOf(";", startIndex);
                if (tempEndIndex >= 0) {
                    endIndex = tempEndIndex;
                }
            }
            return cookie.substring(startIndex, endIndex);
        }
        return null;
    }

    private String setSSOCookie(String cookie, String newSSOCookie) {
        if (cookie != null && newSSOCookie != null) {
            String ssoCookie = getSSOCookie(cookie);
            if (newSSOCookie.isEmpty()) {
                if (cookie.contains(SSO_COOKIE_KEY)) {
                    int startIndex = cookie.indexOf(SSO_COOKIE_KEY);
                    return cookie.substring(0, startIndex);
                }
                return cookie;
            } else if (ssoCookie != null && ssoCookie.length() > 0) {
                return cookie.replaceAll(ssoCookie, newSSOCookie);
            } else if (cookie.contains(SSO_COOKIE_KEY)) {
                int startIndex = cookie.indexOf(SSO_COOKIE_KEY) + SSO_COOKIE_KEY.length() + 1;
                if (startIndex < cookie.length()) {
                    return cookie.substring(0, startIndex) + newSSOCookie + cookie.substring(startIndex);
                } else {
                    return cookie.substring(0, startIndex) + newSSOCookie;
                }
            }
            return cookie + SSO_COOKIE_KEY + "=" + newSSOCookie;
        }
        return null;
    }

    private void readIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Set<String> extraKeys = extras.keySet();
                if (extraKeys.contains(getString(R.string.url_bundle_key))) {
                    loadUrlFromIntent = true;
                    statusUrl = extras.getString(getString(R.string.url_bundle_key));
                }
            }
        }
    }
}
