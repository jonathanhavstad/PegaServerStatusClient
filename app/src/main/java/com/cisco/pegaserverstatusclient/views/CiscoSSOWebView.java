package com.cisco.pegaserverstatusclient.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.cisco.pegaserverstatusclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Created by jonathanhavstad on 11/1/16.
 */

public class CiscoSSOWebView extends WebView {
    private String jsObjectName;
    private String jsText;
    private JsObject jsObject;

    public CiscoSSOWebView(Context context) {
        super(context);
        init();
    }

    public CiscoSSOWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CiscoSSOWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        jsObjectName = getContext().getString(R.string.js_interface_obj_name);
        jsObject = new JsObject();
        addJavascriptInterface(jsObject, jsObjectName);
        loadJsText();
    }

    private void loadJsText() {
        jsText = "";
        final AssetManager assetManager = getResources().getAssets();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                try {
                    in = assetManager.open(getContext().getString(R.string.js_interface_js_json_reader_filename));

                    Scanner scanner = new Scanner(in);
                    StringBuffer sb = new StringBuffer();
                    sb.append(scanner.nextLine());
                    jsText = sb.toString();
                    scanner.close();
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

    public JsObject getJsObject() {
        return jsObject;
    }

    public static class JsObject {
        private String jsonBody;
        private String username;
        @JavascriptInterface
        public void setJsonBody(String jsonBody) {
            this.jsonBody = jsonBody;
        }
        @JavascriptInterface
        public void setUsername(String username) {
            this.username = username;
        }
        public String getJsonBody() { return jsonBody; }
    }
}
