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

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity {
    private static final String HOME_PAGE = "http://www.cisco.com/";
    private static final String LOGIN_URL = "http://www.cisco.com/c/login/index.html?referer=/c/en/us/index.html";
    private static final String SSO_LOGIN_URL = "https://sso.cisco.com/autho/forms/CDClogin.html";
    private static final String CGI_LOGIN_URL = "https://www.cisco.com/cgi-bin/login";
    private static final String LOGIN_REDIRECT_URL = "http://www.cisco.com/c/en/us/index.html";
    private static final String LOGIN_REFERER_URL = "http://www.cisco.com/c/login/index.html?referer=/c/en/us/index.html";
    private static final String LOGOUT_INTERMEDIATE_URL = "http://www.cisco.com/web/fw/lo/logout.html";
    private static final String LOGOUT_COMPLETE_URL = "http://www.cisco.com/web/siteassets/logout/logout.html";
    private static final String LOGOUT_URL = "http://www.cisco.com/autho/logout.html?ReturnUrl=//www.cisco.com/web/fw/lo/logout.html";
    private static final String EVIL_COOKIE_URL = "https://sso.cisco.com/oberr.cgi?status%3D400%20errmsg%3DErrEvilFormLoginCookie%20p1%3D";
    private static final String LOGIN_ACTION_URL = "https://sso.cisco.com/autho/login/loginaction.html";
    private static final String SSO_COOKIE_KEY = "SSOCookie";

    private static final int HTTP_NOT_FOUND_CODE = 404;
    private static final int ACCESS_DATA_REQUEST_CODE = 1000;
    private static final String TAG = "LoginActivity";

    @BindView(R.id.login_web_view)
    WebView webView;

    private boolean beginLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

        enableSpecificWebSettings();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Loading URL: " + url);
                Log.d(TAG, "Site cookie: " + CookieManager.getInstance().getCookie(url));
                Log.d(TAG, "SSO Cookie: " + getSSOCookie(CookieManager.getInstance().getCookie(url)));

                CookieManager.getInstance().setAcceptCookie(true);

                String newUrl = null;
                if (url.equals(LOGOUT_COMPLETE_URL)) {
                    newUrl = HOME_PAGE;
                } else if (url.equals(HOME_PAGE)) {
                    newUrl = LOGIN_URL;
                } else if (url.equals(SSO_LOGIN_URL)) {
                    beginLogin = true;
                    view.setVisibility(View.VISIBLE);
                } else if (beginLogin) {
                    view.setVisibility(View.INVISIBLE);
                    beginLogin = false;
                    launchMainActivity();
                }

                if (newUrl != null) {
                    Log.d(TAG, "Redirecting to: " + newUrl);
                    view.loadUrl(newUrl);
                } else {
                    super.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(TAG, "Received " + errorCode + " error loading page " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "URL finished loading: " + url);
                CookieManager.getInstance().setAcceptCookie(true);
            }
        });

        beginLogin = false;

        webView.setVisibility(View.INVISIBLE);
        webView.loadUrl(LOGOUT_URL);
    }

    private void enableSpecificWebSettings() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.clearCache(true);

        CookieManager.getInstance().setAcceptCookie(true);
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, ACCESS_DATA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCESS_DATA_REQUEST_CODE) {
            beginLogin = false;
            webView.clearCache(true);
            webView.setVisibility(View.INVISIBLE);
            webView.loadUrl(LOGOUT_URL);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
}
