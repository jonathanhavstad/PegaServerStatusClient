package com.cisco.pegaserverstatusclient.views;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by jonathanhavstad on 11/1/16.
 */

public class CiscoLoginFormWebView extends WebView {
    public CiscoLoginFormWebView(Context context) {
        super(context);
    }

    public CiscoLoginFormWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CiscoLoginFormWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        super.postUrl(url, postData);
    }
}
