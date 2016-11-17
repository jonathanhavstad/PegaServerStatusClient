package com.cisco.pegaserverstatusclient.activities;

import android.content.Intent;
import android.os.Build;
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
    private boolean beginLogin;
    private boolean beginAuthentication;
    private boolean beginDataAcquisition;
    private boolean loadUrlFromIntent;
    private String statusUrl;
    private boolean appsActivityLaunched;

    @BindView(R.id.login_web_view)
    CiscoSSOWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        readIntent(getIntent());

        webView.setVisibility(View.INVISIBLE);

        loadData(statusUrl);

        Intent serviceIntent = new Intent(this, RegistrationIntentService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCESS_DATA_REQUEST_CODE) {
            if (resultCode == ServerAppsActivity.RESULT_CLOSED) {
                finish();
            } else if (resultCode != ServerAppsActivity.RESULT_LOGIN) {
                init();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadData(final String url) {
        CiscoSSOWebService ciscoSSOWebService = new CiscoSSOWebService(this, webView);

        ciscoSSOWebService.loadData(url,
                new OnDataLoadedListener() {
                    @Override
                    public void send(JSONArray jsonArray) {
                        Log.d(TAG, "Received JSON array: " + jsonArray.toString());
                        launchMainActivity(url);
                        appsActivityLaunched = true;
                    }

                    @Override
                    public void waitForLogin() {
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

    private void cancelMainActivity(String url) {
        Intent intent = new Intent(this, ServerAppsActivity.class);
        intent.putExtra(getString(R.string.status_url_bundle_key), url);
        intent.putExtra(getString(R.string.stop_activity_bundle_key), url);
        startActivityForResult(intent, ACCESS_DATA_REQUEST_CODE);
    }

    private void readIntent(Intent intent) {
        statusUrl = getString(R.string.cisco_login_url);
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
