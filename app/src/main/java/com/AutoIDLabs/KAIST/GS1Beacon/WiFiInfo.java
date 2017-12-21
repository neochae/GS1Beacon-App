package com.AutoIDLabs.KAIST.GS1Beacon;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by neochae on 2017. 12. 22..
 */

public class WiFiInfo {
    private static String TAG = "WiFiActivity";

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
        SsidConverter converter = new SsidConverter('!', 90);
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
