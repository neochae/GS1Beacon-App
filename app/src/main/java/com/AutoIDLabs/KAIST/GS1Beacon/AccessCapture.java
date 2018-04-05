package com.AutoIDLabs.KAIST.GS1Beacon;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by neochae on 2017. 12. 21..
 */

public class AccessCapture {
    private Context mContext;
    private String mGs1Code;
    private String mUrn;
    private String mUrl;
    private String sampleProduct = "(01)08800026916109";

    private String mSampleDate = "2017-12-22T11:25:08.720";
    private String mSampleGtin = "urn:epc:id:sgtin:88000269.01610.000";
    private String mSampleGln = "urn:epc:id:sgln:88000269.1601.000";
    private String mSampleUrl = "https://www.google.co.kr/";
    private String mSampleUser = "50:55:27:ff:29:40";

    public AccessCapture(Context ctx, String gs1Code, String urn, String url) {
        mContext = ctx;
        mGs1Code = gs1Code;
        mUrn = urn;
        mUrl = url;
    }

    public void capture() {
        new Thread() {
            public void run() {
                WifiManager manager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();

                //String address = info.getMacAddress();
                String address = getMacAddress("wlan0");
                String productUrn = getUrn(sampleProduct, 8);
                String gs1APUrn = getUrn(mGs1Code, 8);
                String currentTime = getCurrentTime();

                Log.i("EPCIS", address + " " + productUrn + " " + gs1APUrn + " " + currentTime + " " + mUrl);

                //TODO
                //1. Event GTIN urn change (Product)
                //2. readpoint, bizlocation change (AP)
                //3. eventTime (current)
                //4. user information mac address

                //File file = new File("/data/local/tmp/access_example2.xml");

                try {
                    HttpClient client = new DefaultHttpClient();
                    String postURL = "http://14.63.168.75:8080/epcis/Service/EventCapture";
                    HttpPost post = new HttpPost(postURL);
                    StringBuilder text = new StringBuilder();

                    try {
                        AssetManager assetManager = mContext.getAssets();
                        Reader reader = new InputStreamReader(assetManager.open("access_example.xml"));
                        BufferedReader br = new BufferedReader(reader);
                        String line;

                        while ((line = br.readLine()) != null) {
                            text.append(line);
                            text.append('\n');
                        }

                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String samepleEvent = text.toString();
                    samepleEvent = samepleEvent.replaceAll(mSampleDate, currentTime);
                    samepleEvent = samepleEvent.replaceAll(mSampleGtin, productUrn);
                    samepleEvent = samepleEvent.replaceAll(mSampleGln, gs1APUrn);
                    samepleEvent = samepleEvent.replaceAll(mSampleUser, address);
                    samepleEvent = samepleEvent.replaceAll(mSampleUrl, mUrl);

                    StringEntity entity = new StringEntity(samepleEvent, "UTF-8");
                    post.setEntity(entity);
                    HttpResponse response = client.execute(post);
                    HttpEntity resEntity = response.getEntity();

                    if (resEntity != null) {
                        Log.i("EPCIS", EntityUtils.toString(resEntity));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public String getUrn(String code, int companyPrefix) {
        String urn = "";
        String[] arr = code.split("[\\(\\)]");

        if (arr.length < 3) {
            return "";
        }

        if (arr[1].equals("414")) {
            urn ="urn:epc:id:sgln:" + arr[2].substring(0, companyPrefix) + "." + arr[2].substring(companyPrefix, arr[2].length() -1) + ".000";
        } else if (arr[1].equals("01")) {
            urn ="urn:epc:id:sgtin:" + arr[2].substring(1, companyPrefix+1) + "." +  arr[2].substring(0, 1) + arr[2].substring(companyPrefix+1, arr[2].length() -1) + ".000";
        }

        return urn;
    }

    public String getCurrentTime() {
        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
        String fHour = new SimpleDateFormat("HH:mm:ss.SSS").format(cDate);

        return fDate + "T" + fHour;
    }

    public String getMacAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions

        return "";
    }
}
