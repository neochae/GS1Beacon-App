package com.AutoIDLabs.KAIST.GS1Beacon;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.net.wifi.ScanResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;


public class WiFiActivity extends PreferenceActivity {
    private static String TAG = "WiFiActivity";
    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;
    private Context mContext;
    private HashMap<String, WiFiInfo> mWifis = new HashMap<String, WiFiInfo>();
    private Handler mLoaderHandler;
    private boolean mProcessing;
    PreferenceCategory mGS1Preference;
    PreferenceCategory mNormalPreference;

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
        }

        //Initialize preference
        addPreferencesFromResource(R.xml.preferences);
        mGS1Preference = (PreferenceCategory) findPreference("gs1_category");
        mNormalPreference = (PreferenceCategory) findPreference("normal_category");
    }

    protected void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiScanReceiver, filter);

        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.startScan();
            Log.d(TAG, "Wifi AP scan start..");
        }
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
            if (wifi.isGS1WiFi()) {
                Log.d(TAG, "GS1 AP 선택");

                Intent intent = new Intent(WiFiActivity.this, ServiceActivity.class);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, wifi.apSsid);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "("+wifi.gs1Ai+")"+wifi.gs1Code);

                Bundle args1 = new Bundle();
                args1.putSerializable("ARRAYLIST1", (Serializable) new ArrayList<String>());
                Bundle args2 = new Bundle();
                args2.putSerializable("ARRAYLIST2",(Serializable) new ArrayList<String>());

                intent.putExtra("BUNDLE1", args1);
                intent.putExtra("BUNDLE2", args2);

                startActivity(intent);
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
                Toast.makeText(WiFiActivity.this, mWifis.size() + "개의 WiFi AP 정보를 수집하였습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makePreferences() {
        Log.d(TAG, "making preference..");

        mGS1Preference.removeAll();
        mNormalPreference.removeAll();

        for(String key : mWifis.keySet() ){
            WiFiInfo wifi = mWifis.get(key);
            Preference wifiPreference = makePreference(wifi);

            if (wifi.isGS1WiFi()) {
                mGS1Preference.addPreference(wifiPreference);
            } else {
                mNormalPreference.addPreference(wifiPreference);
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

        //test data
        mWifis.put("00:1c:42:00:00:07", new WiFiInfo("00:1c:42:00:00:07", "NEO (01)0000000013200", -10));
        mWifis.put("00:1c:42:00:00:08", new WiFiInfo("00:1c:42:00:00:08", "NEO (01)0000000000132", -10));
        mWifis.put("00:1c:42:00:00:09", new WiFiInfo("00:1c:42:00:00:09", "SMITH (01)8854321000081", -20));
    }

    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                getWiFiAP();
                updateWiFi();
            }else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
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

    class WiFiInfo {
        public String apBssid;
        public String apSsid;
        public int apRssi;

        //GS1 WiFi only
        public String gs1Ai;
        public String gs1Code;
        private String gs1Pattern = ".*\\((01|414|255|8017)\\)(\\d+)$";

        public WiFiInfo(String bssid, String ssid, int rssi) {
            apBssid = bssid;
            apSsid = ssid;
            apRssi = rssi;
            gs1Ai = null;
            gs1Code = null;

            //GS1 WiFi AP
            if (getGS1Info(apSsid)) {
                Log.e(TAG, "GS1 WiFi detected AI=" + gs1Ai + ", GS1=" + gs1Code);
            }
        }

        public String toString() {
            return "BSSID: " + apBssid + ", SSID: " + apSsid + ", RSSI: " + apRssi + ", gs1Code: " + gs1Code;
        }

        public boolean getGS1Info(String ssid) {
            Pattern pattern = Pattern.compile(gs1Pattern);
            Matcher matcher = pattern.matcher(ssid);

            if (matcher.find()) {
                gs1Ai = matcher.group(1);
                gs1Code = matcher.group(2);
                return true;
            }

            return false;
        }

        public boolean isGS1WiFi() {
            if (gs1Code != null) {
                return true;
            }

            return false;
        }

        public void testPattern() {
            List<String> testSet = new ArrayList<String>();
            testSet.add("NEO");
            testSet.add("(01)01234567890234");
            testSet.add("NEO (01)01234567890234");
            testSet.add("(414)01234567890234");
            testSet.add("NEO (414)01234567890234");
            testSet.add("NEO (a414)01234567890234");
            testSet.add("N11EO (a414)01234567890234");
            testSet.add("N11EO (a414)01234567890234");
            testSet.add("N11EO (a414)01234567890234a");
            testSet.add("N11EO (a414)01234567890234  1");
            testSet.add("N11EO (a414)01234567890234  (01)1");
            testSet.add("N11EO (a414)01234567890234  (01)");
            testSet.add("N11EO 010 01234567890234");

            for (String test : testSet) {
                if (Pattern.matches(gs1Pattern, test)) {
                    Log.e(TAG, test + ",  gs1 match");
                } else {
                    Log.e(TAG, test + ",  gs1 does not match");
                }
            }
        }
    }

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


