package com.AutoIDLabs.KAIST.GS1Beacon;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.os.Bundle;
import android.net.wifi.ScanResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;
import android.widget.Toast;


public class WiFiActivity extends PreferenceActivity {
    private static String TAG = "WiFiActivity";
    private WifiManager mWifiManager;
    private Context mContext;
    private HashMap<String, WiFiInfo> mWifis = new HashMap<String, WiFiInfo>();
    private Handler mLoaderHandler;
    private boolean mProcessing;
    private long mScanStart;
    private ProgressDialog mProgressDialog;
    PreferenceCategory mGS1Preference;
    SwitchPreference mNormalPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HandlerThread handlerThread = new HandlerThread("WiFi Loaders", Thread.NORM_PRIORITY);
        handlerThread.start();
        mLoaderHandler = new WiFiLoaderHandler(handlerThread.getLooper());

        mContext = getApplicationContext();
        mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
            Log.d(TAG, "Wifi AP request enable");
        } else {
            mWifiManager.startScan();
            mScanStart = System.currentTimeMillis();
            Log.d(TAG, "Wifi AP scan start");
            mProgressDialog = ProgressDialog.show(WiFiActivity.this, "", "GS1 AP정보를 수집 중입니다.", true);
            mProgressDialog.show();
        }

        //Initialize preference
        addPreferencesFromResource(R.xml.preferences);
        mGS1Preference = (PreferenceCategory) findPreference("gs1_category");
        mNormalPreference = (SwitchPreference) findPreference("backgroundScanning");
        mNormalPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals("true")) {
                    Intent intent = new Intent(WiFiActivity.this, BackgroundScanner.class);
                    startService(intent);
                } else {
                    Intent intent = new Intent(WiFiActivity.this, BackgroundScanner.class);
                    stopService(intent);
                }
                return true;
            }
        });
        if (mNormalPreference.isChecked()) {
            Intent intent = new Intent(WiFiActivity.this, BackgroundScanner.class);
            startService(intent);
        }
    }

    protected void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiScanReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContext.unregisterReceiver(mWifiScanReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey() != null) {
            WiFiInfo wifi = mWifis.get(preference.getKey());
            if (wifi != null && wifi.isGS1WiFi()) {
                Log.d(TAG, "GS1 AP 선택");

                Intent intent = new Intent(WiFiActivity.this, ServiceActivity.class);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, wifi.apSsid);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "("+wifi.gs1Ai+")"+wifi.gs1Code + "\n");

                Bundle args1 = new Bundle();
                args1.putSerializable("ARRAYLIST1", (Serializable) new ArrayList<String>());
                Bundle args2 = new Bundle();
                args2.putSerializable("ARRAYLIST2",(Serializable) new ArrayList<String>());

                intent.putExtra("BUNDLE1", args1);
                intent.putExtra("BUNDLE2", args2);

                startActivity(intent);
            } else if (preference.getKey().equals("refreshScan")) {
                if(mWifiManager.isWifiEnabled()) {
                    mWifiManager.startScan();
                    Log.d(TAG, "Wifi AP scan re-start");
                }
                Log.d(TAG, "Wifi AP explicit scanning..");
                mScanStart = System.currentTimeMillis();
                mProgressDialog = ProgressDialog.show(WiFiActivity.this, "", "GS1 AP정보를 수집 중입니다.", true);
                mProgressDialog.show();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateWiFi() {
        mLoaderHandler.post(new Runnable() {
            @Override
            public void run() {
                mProcessing = true;
                makePreferences();
                mProcessing = false;
            }
        });
    }

    private void makePreferences() {
        Log.d(TAG, "making preference..");

        mGS1Preference.removeAll();

        for(String key : mWifis.keySet() ){
            WiFiInfo wifi = mWifis.get(key);
            Preference wifiPreference = makePreference(wifi);

            if (wifi.gs1Ai != null) {
                if (wifi.gs1Ai.equals("01")) {
                    wifiPreference.setIcon(R.drawable.gtin);
                } else if (wifi.gs1Ai.equals("414")) {
                    wifiPreference.setIcon(R.drawable.gln);
                }
            }

            if (wifi.isGS1WiFi()) {
                mGS1Preference.addPreference(wifiPreference);
            } else {
                //mNormalPreference.addPreference(wifiPreference);
            }
        }

        Log.d(TAG, "making preference complete");
    }

    private Preference makePreference(WiFiInfo wifi) {
        Preference wifiPreference = new Preference(WiFiActivity.this);
        wifiPreference.setKey(wifi.apBssid);
        wifiPreference.setTitle(wifi.apSsid);

        //TODO, Query dns..
        return wifiPreference;
    }

    public void getWiFiAP(){
        List<ScanResult> scanResult;
        scanResult = mWifiManager.getScanResults();

        mWifis.clear();
        for (int i = 0; i < scanResult.size(); i++) {
            ScanResult result = scanResult.get(i);
            //Log.d(TAG, (i + 1) + ". SSID : " + result.SSID + "\t\t RSSI : " + result.level + " dBm, mac : " + result.BSSID + "\n");
            WiFiInfo wifiInfo = new WiFiInfo(result.BSSID, result.SSID, result.level);
            mWifis.put(wifiInfo.apBssid, wifiInfo);
        }

        long scan = System.currentTimeMillis();
        Log.d(TAG, "Wifi AP scan end, time=" + (scan-mScanStart));
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        //mWifis.put("00:1c:42:00:00:0d", new WiFiInfo("00:1c:42:00:00:0d", "방탄 (414)8800026916017", -20));
        //mWifis.put("00:1c:42:00:00:0e", new WiFiInfo("00:1c:42:00:00:0e", "스벅 (01)08800026916109", -20));
    }

    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                getWiFiAP();
                updateWiFi();
            } else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                switch(mWifiManager.getWifiState()) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.d(TAG, "WIFI disabled..");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d(TAG, "WIFI enabled..");
                        mWifiManager.startScan();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    class WiFiLoaderHandler extends Handler {
        public WiFiLoaderHandler (Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}


