package com.cisco.pegaserverstatusclient.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.cisco.pegaserverstatusclient.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.cisco.pegaserverstatusclient.background.services.RegistrationIntentService;
import com.cisco.pegaserverstatusclient.listeners.OnDataLoadedListener;
import com.cisco.pegaserverstatusclient.rest.services.CiscoSSOWebService;
import com.cisco.pegaserverstatusclient.views.CiscoSSOWebView;
import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;

import java.util.Set;

import io.fabric.sdk.android.Fabric;

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
    private boolean beginDataAcquisition;
    private boolean loadUrlFromIntent;
    private String statusUrl;

    @BindView(R.id.login_web_view)
    CiscoSSOWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        readIntent(getIntent());

        webView.setVisibility(View.INVISIBLE);

        loadLoginUrl();

        Intent serviceIntent = new Intent(this, RegistrationIntentService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCESS_DATA_REQUEST_CODE) {
            loadLoginUrl();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadLoginUrl() {
        CiscoSSOWebService ciscoSSOWebService = new CiscoSSOWebService(this, webView);

        ciscoSSOWebService.loadDataAfterLogout(getString(R.string.cisco_login_url),
                new OnDataLoadedListener() {
                    @Override
                    public void send(JSONArray jsonArray) {
                        Log.d(TAG, "Received JSON array: " + jsonArray.toString());
                        launchMainActivity(getString(R.string.cisco_login_url));
                    }

                    @Override
                    public void error(String error) {
                        Log.e(TAG, "Error receiving JSON data: " + error);
                    }
                });
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
