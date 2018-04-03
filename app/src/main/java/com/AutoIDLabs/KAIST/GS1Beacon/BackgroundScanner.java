package com.AutoIDLabs.KAIST.GS1Beacon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BackgroundScanner extends Service {
    private static String TAG = "BackgroundScanner";
    private WifiManager mWifiManager;
    private HashMap<String, WiFiInfo> mNotification = new HashMap<String, WiFiInfo>();
    private Handler mLoaderHandler;

    public BackgroundScanner() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        HandlerThread handlerThread = new HandlerThread("WiFi Loaders", Thread.NORM_PRIORITY);
        handlerThread.start();
        mLoaderHandler = new BackgroundScanner.WiFiLoaderHandler(handlerThread.getLooper());

        Intent pendingIntent = new Intent(this, WiFiActivity.class);
        pendingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("GS1 AP Scanner")
                .setContentText("백그라운드에서 동작중입니다.")
                .setVibrate(new long[]{0, 3000})
                .setSmallIcon(R.drawable.gs1ap)
                .setContentIntent(PendingIntent.getActivity(BackgroundScanner.this, 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .build();
        Log.d("BackgroundScanner", "starting in the Foreground");
        startForeground(startId, notification);

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        BackgroundScanner.this.registerReceiver(mWifiScanReceiver, filter);

        mWifiManager = (WifiManager)BackgroundScanner.this.getSystemService(Context.WIFI_SERVICE);
        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
            Log.d(TAG, "Wifi AP request enable");
        } else {
            mWifiManager.startScan();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (mNotification) {
            Set set = mNotification.keySet();
            Iterator iterator = set.iterator();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                notificationManager.cancel(key.hashCode());
            }
        }
    }

    public Intent getDetailIntent(String ssid, String ai, String code) {
        Intent intent = new Intent(BackgroundScanner.this, ServiceActivity.class);
        intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, ssid);
        intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "("+ai+")"+code + "\n");

        Bundle args1 = new Bundle();
        args1.putSerializable("ARRAYLIST1", (Serializable) new ArrayList<String>());
        Bundle args2 = new Bundle();
        args2.putSerializable("ARRAYLIST2",(Serializable) new ArrayList<String>());

        intent.putExtra("BUNDLE1", args1);
        intent.putExtra("BUNDLE2", args2);

        Log.d(TAG, "getDetailIntent intent notified! " + intent + ", code=" + code);

        return intent;
    }

    Runnable mChecker = new Runnable() {
        public void run() {
            List<ScanResult> scanResult;
            HashMap<String, WiFiInfo> newList = new HashMap<String, WiFiInfo>();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            scanResult = mWifiManager.getScanResults();
            for (int i = 0; i < scanResult.size(); i++) {
                ScanResult result = scanResult.get(i);
                WiFiInfo wifiInfo = new WiFiInfo(result.BSSID, result.SSID, result.level);
                newList.put(wifiInfo.apBssid, wifiInfo);

                //Add new GS1 AP
                if (mNotification.get(wifiInfo.apBssid) == null) {
                    if (wifiInfo.isGS1WiFi()) {
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(BackgroundScanner.this);

                        int requestID = (int) System.currentTimeMillis();
                        Intent intent = getDetailIntent(wifiInfo.apSsid, wifiInfo.gs1Ai, wifiInfo.gs1Code);
                        PendingIntent pendingIntent = PendingIntent.getActivity(BackgroundScanner.this, requestID, intent, 0);

                        mBuilder.setContentIntent(pendingIntent);
                        if (wifiInfo.gs1Ai.equals("01")) {
                            mBuilder.setSmallIcon(R.drawable.gtin);
                        } else if (wifiInfo.gs1Ai.equals("414")) {
                            mBuilder.setSmallIcon(R.drawable.gln);
                        }
                        mBuilder.setContentTitle("GS1 AP");
                        mBuilder.setContentText(wifiInfo.apSsid);

                        notificationManager.notify(wifiInfo.apBssid.hashCode(), mBuilder.build());
                        synchronized (mNotification) {
                            mNotification.put(wifiInfo.apBssid, wifiInfo);
                        }

                        Toast toast = Toast.makeText(getApplicationContext(),
                                "새로운 GS1 AP 서비스가 검색되었습니다", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        Log.d(TAG, "WIFI backgorund scan, new notified!" + wifiInfo + ", id=" + wifiInfo.apBssid.hashCode());
                    }
                } else {
                    Log.d(TAG, "WIFI backgorund scan, already notified!");
                }
            }

            //Check Already Service
            synchronized (mNotification) {
                ArrayList<String> removeList = new ArrayList<String>();
                Set set = mNotification.keySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    if (newList.get(key) == null || !newList.get(key).isGS1WiFi()) {
                        Log.d(TAG, "WIFI backgorund scan, remove notification");
                        notificationManager.cancel(key.hashCode());
                        removeList.add(key);
                    }
                }
                for(int i = 0; i < removeList.size(); i++) {
                    mNotification.remove(removeList.get(i));
                }
            }

            //Scan Again..
            Log.d(TAG, "WIFI backgorund scan, schedule");
            mLoaderHandler.postDelayed(new Runnable() {
                public void run() {
                    mWifiManager.startScan();
                }
            }, 10000);
        }
    };

    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                mLoaderHandler.removeCallbacks(mChecker);
                mLoaderHandler.postDelayed(mChecker, 1000);
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
