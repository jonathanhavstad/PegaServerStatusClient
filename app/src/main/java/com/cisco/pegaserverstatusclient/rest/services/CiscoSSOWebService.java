package com.cisco.pegaserverstatusclient.rest.services;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.listeners.OnDataLoadedListener;
import com.cisco.pegaserverstatusclient.views.CiscoSSOWebView;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by jonathanhavstad on 11/8/16.
 */

public class CiscoSSOWebService {
    private String LOGOUT_URL;
    private String HOME_PAGE;
    private String SSO_LOGIN_URL;
    private String LOGOUT_COMPLETE_URL;
    private String SSO_COOKIE_KEY;
    private String SSO_LOGGED_OUT_VALUE;

    private CiscoSSOWebView webView;
    private OnDataLoadedListener onDataLoadedListener;

    private String dataUrl;
    private boolean beginLogin;
    private boolean beginDataAcquisition;

    private Activity context;
    private boolean launchFromBgThread;

    public CiscoSSOWebService(final Activity context) {
        this.context = context;
        this.launchFromBgThread = true;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CiscoSSOWebService.this.webView = new CiscoSSOWebView(CiscoSSOWebService.this.context);
                init(CiscoSSOWebService.this.context);
            }
        });
    }

    public CiscoSSOWebService(Activity context, CiscoSSOWebView webView) {
        this.context = context;
        this.webView = webView;
        this.launchFromBgThread = true;
        init(context);
    }

    public void loadDataAfterLogout(String dataUrl, OnDataLoadedListener onDataLoadedListener) {
        this.dataUrl = dataUrl;
        loadUrl(LOGOUT_URL, onDataLoadedListener, false, false);
    }

    public void loadData(String dataUrl, OnDataLoadedListener onDataLoadedListener) {
        loadUrl(dataUrl, onDataLoadedListener, true, false);
    }

    private void loadUrl(final String url,
                         OnDataLoadedListener onDataLoadedListener,
                         boolean beginLogin,
                         boolean beginDataAcquisition) {
        this.beginLogin = beginLogin;
        this.beginDataAcquisition = beginDataAcquisition;
        this.onDataLoadedListener = onDataLoadedListener;
        this.dataUrl = url;
        if (webView != null) {
            if (launchFromBgThread && context != null) {
                 context.runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         CiscoSSOWebService.this.webView.loadUrl(url);
                     }
                 });
            } else {
                webView.loadUrl(url);
            }
        }
    }

    private void init(Context context) {
        loadWebValues(context);

        enableSpecificWebSettings();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                CookieManager.getInstance().setAcceptCookie(true);

                String newUrl = null;
                if (url.equals(LOGOUT_COMPLETE_URL)) {
                    newUrl = HOME_PAGE;
                } else if (url.equals(HOME_PAGE)) {
                    newUrl = dataUrl;
                } else if (url.equals(SSO_LOGIN_URL)) {
                    beginDataAcquisition = true;
                    view.setVisibility(View.VISIBLE);
                } else if (beginLogin && url.equals(dataUrl)) {
                    beginDataAcquisition = true;
                    view.setVisibility(View.INVISIBLE);
                }

                if (newUrl != null) {
                    view.loadUrl(newUrl);
                } else {
                    super.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().setAcceptCookie(true);
                String ssoCookie = getSSOCookie(CookieManager.getInstance().getCookie(url));
                if (beginDataAcquisition && url.equals(dataUrl) && isLoggedIn(ssoCookie)) {
                    evaluateStatusCode();
                }
            }
        });
    }

    private void enableSpecificWebSettings() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.clearCache(true);

        CookieManager.getInstance().setAcceptCookie(true);
    }

    private void loadWebValues(Context context) {
        LOGOUT_URL = context.getString(R.string.cisco_logout_url);
        HOME_PAGE = context.getString(R.string.cisco_home_page_url);
        SSO_LOGIN_URL = context.getString(R.string.cisco_sso_login_url);
        LOGOUT_COMPLETE_URL = context.getString(R.string.cisco_logout_complete_url);
        SSO_COOKIE_KEY = context.getString(R.string.sso_cookie_key);
        SSO_LOGGED_OUT_VALUE = context.getString(R.string.sso_loggedout_value);
    }

    private void evaluateStatusCode() {
        webView.evaluateJavascript(webView.getJsStatusCodeText(), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (webView.getJsObject().getStatusCode() == 200) {
                    evaluateJson();
                }
            }
        });
    }

    private void evaluateJson() {
        webView.evaluateJavascript(webView.getJsJsonText(), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (onDataLoadedListener != null) {
                    try {
                        String jsonBody = webView.getJsObject().getJsonBody();
                        if (jsonBody != null) {
                            JSONArray jsonArray =
                                    new JSONArray(webView.getJsObject().getJsonBody());
                            onDataLoadedListener.send(jsonArray);
                        }
                    } catch (JSONException e) {
                        onDataLoadedListener.error(e.toString());
                    }
                }
            }
        });
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

    private boolean isLoggedIn(String ssoCookie) {
        if (ssoCookie == null || ssoCookie.contains(SSO_LOGGED_OUT_VALUE)) {
            return false;
        }
        return true;
    }
}
