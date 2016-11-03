package com.cisco.pegaserverstatusclient.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebMessage;
import android.webkit.WebView;

import com.cisco.pegaserverstatusclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by jonathanhavstad on 11/1/16.
 */

public class CiscoLoginFormWebView extends WebView {
    private String jsObjectName;
    private String jsText;

    public CiscoLoginFormWebView(Context context) {
        super(context);
        init();
    }

    public CiscoLoginFormWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CiscoLoginFormWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        jsObjectName = getContext().getString(R.string.js_interface_obj_name);
        addJavascriptInterface(new JsObject(), jsObjectName);
//        loadJsText();
    }

    private void loadJsText() {
        final AssetManager assetManager = getResources().getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                try {
                    in = assetManager.open(getContext().getString(R.string.js_interface_js_text_fname));
//                    Scanner scanner = new Scanner(in);
//                    StringBuffer sb = new StringBuffer();
//                    sb.append(getContext().getString(R.string.js_interface_js_prefix));
//                    sb.append(scanner.nextLine());
//                    jsText = sb.toString();
//                    scanner.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public String getJsText() {
        return jsText;
    }

    public static class JsObject {
        private String username;
        @JavascriptInterface
        public void setUsername(String username) {
            this.username = username;
        }
    }
}
