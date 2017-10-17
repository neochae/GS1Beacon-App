package com.AutoIDLabs.KAIST.GS1Beacon;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by SNAIL on 2016-02-15.
 */
public class WebActivity extends AppCompatActivity {

    public static final String EXTRAS_SERVICE_URL = "SERVICE_URL";

    private String mWebUrl;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_main);

        setContentView(R.layout.web_main);

        final Intent intent = getIntent();
        mWebUrl = intent.getStringExtra(EXTRAS_SERVICE_URL);

        setLayout();

        // Focusable
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(true);
        // Javascript enable
        mWebView.getSettings().setJavaScriptEnabled(true);
        // Ser service url
        mWebView.loadUrl(mWebUrl);
        // Set WebViewClient
        mWebView.setWebViewClient(new WebViewClientClass());

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class WebViewClientClass extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /*
     * Layout
     */
    private void setLayout(){
        mWebView = (WebView) findViewById(R.id.webView);
    }
}