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
import java.math.BigInteger;


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
            if (wifi != null && wifi.isGS1WiFi()) {
                Log.d(TAG, "GS1 AP 선택");

                Intent intent = new Intent(WiFiActivity.this, ServiceActivity.class);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, wifi.apSsid);
                intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "("+wifi.gs1Ai+")"+wifi.gs1Code + "0");

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
        mWifis.put("00:1c:42:00:00:0a", new WiFiInfo("00:1c:42:00:00:0a", "GS1AP 1 (01)50614141322607", -20));
        mWifis.put("00:1c:42:00:00:0b", new WiFiInfo("00:1c:42:00:00:0b", "GS1AP 2 {\"}\"&6RH-Qx", -20));
        mWifis.put("00:1c:42:00:00:0c", new WiFiInfo("00:1c:42:00:00:0c", "GS1AP 3 {\"}\"&6RH-Qx{6}5r]orB", -20));
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

    class WiFiInfo {
        public String apBssid;
        public String apSsid;
        public int apRssi;

        //GS1 WiFi only
        public String gs1Ai;
        public String gs1Code;
        public String gs1Serial;
        private String gs1Pattern = ".*\\((01|21|414|255|8017)\\)(\\d+)$";
        private String gs1PatternEx = ".*\\{(\"|%W|#l|z\\()\\}(.+)$";
        private String gs1SerialPatternEx = "\\{(6)\\}(.+)$";

        public WiFiInfo(String bssid, String ssid, int rssi) {
            apBssid = bssid;
            apSsid = ssid;
            apRssi = rssi;
            gs1Ai = null;
            gs1Code = null;
            gs1Serial = null;

            //GS1 WiFi AP
            if (getGS1Info(apSsid)) {
                if (gs1Serial == null) {
                    Log.d(TAG, "GS1 WiFi detected AI=" + gs1Ai + ", GS1=" + gs1Code);
                } else {
                    Log.d(TAG, "GS1 WiFi detected AI=" + gs1Ai + ", GS1=" + gs1Code + ", SERIAL=" + gs1Serial);
                }
            }
        }

        public String toString() {
            return "BSSID: " + apBssid + ", SSID: " + apSsid + ", RSSI: " + apRssi + ", gs1Code: " + gs1Code;
        }

        public boolean getGS1Info(String ssid) {
            //(, Normal
            Pattern pattern = Pattern.compile(gs1Pattern);
            Matcher matcher = pattern.matcher(ssid);
            if (matcher.find()) {
                gs1Ai = matcher.group(1);
                gs1Code = matcher.group(2);
                return true;
            }

            //{, With serial
            CodeConverter converter = new CodeConverter('!', 90);
            pattern = Pattern.compile(gs1SerialPatternEx);
            matcher = pattern.matcher(ssid);
            if (matcher.find()) {
                gs1Serial = converter.convertCodeToInt(matcher.group(2));
                int index = ssid.lastIndexOf('{');
                ssid = ssid.substring(0, index);
            }

            //{
            pattern = Pattern.compile(gs1PatternEx);
            matcher = pattern.matcher(ssid);

            if (matcher.find()) {
                gs1Ai = converter.convertCodeToInt(matcher.group(1));
                gs1Code = converter.convertCodeToInt(matcher.group(2));

                if (gs1Ai.equals("1")) {
                    gs1Ai = "01";
                }
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
            testSet.add("(01)50614141322607");
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
                    Log.d(TAG, test + ",  gs1 match");
                } else {
                    Log.d(TAG, test + ",  gs1 does not match");
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


    public class CodeConverter {
        public int mCodeBase = (int)'!';
        public int mCodeNum = 90;

        public CodeConverter(char codeBase, int codeNum) {
            mCodeBase = (int)codeBase;
            mCodeNum = codeNum;
        }

        String convertCodeToInt(String code) {
            BigInteger number = new BigInteger("0");
            BigInteger addFactor = new BigInteger("1");
            char []codes = code.toCharArray();
            for (int i = codes.length - 1; i >= 0; i--) {
                number = number.add(addFactor.multiply(BigInteger.valueOf((int)codes[i] - mCodeBase)));
                addFactor = addFactor.multiply(BigInteger.valueOf(mCodeNum));
            }

            return number.toString();
        }

        String convertIntToCode(String num) {
            int size = 0;
            BigInteger number = new BigInteger(num);
            while(number.compareTo(BigInteger.valueOf(0)) != 0) {
                number = number.divide(BigInteger.valueOf(mCodeNum));
                size++;
            }

            char []codes = new char[size];
            number = new BigInteger(num);
            while(number.compareTo(BigInteger.valueOf(0)) != 0) {
                int remain = number.mod(BigInteger.valueOf(mCodeNum)).intValue();
                number = number.divide(BigInteger.valueOf(mCodeNum));
                codes[--size] = (char)(mCodeBase + remain);
            }

            return String.valueOf(codes);
        }
    }
}


