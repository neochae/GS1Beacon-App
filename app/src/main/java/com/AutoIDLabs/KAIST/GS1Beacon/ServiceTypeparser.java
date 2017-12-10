package com.AutoIDLabs.KAIST.GS1Beacon;

import java.util.ArrayList;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import android.os.Handler;
import android.util.Log;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;


public class ServiceTypeParser implements Runnable{
    private static String TAG = "ServiceTypeParser";
    public static final String ID_TAGS = "ServiceTypeIdentifier";
    public static final String ICON_TAGS = "ICON";
    public static final String DESC_TAGS = "DESC";
    HashMap<String, String> mInterestTags;

    private Handler mHandler;
    private String mAddr;

    public ServiceTypeParser(String addr, Handler handler) {
        mAddr = addr;
        mHandler = handler;
    }

    public XmlPullParser getXMLParser(String type){
        try {
            URL targetURL = new URL(mAddr);
            InputStream is = targetURL.openStream();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            parser.setInput(is, type);
            return parser;
        }catch(Exception e){
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    public void startParsing() {
        String tag;
        XmlPullParser parser = getXMLParser("utf-8");

        if(parser == null) {
            mInterestTags = null;
            Log.d(TAG, "Paser Object is null");
        } else{
            mInterestTags = new HashMap();
            try{
                int tagIdentifier = 0;
                int parserEvent = parser.getEventType();

                while (parserEvent != XmlPullParser.END_DOCUMENT){
                    switch(parserEvent){
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            tag = parser.getName();
                            if (tag.equals(ID_TAGS)) {
                                tagIdentifier = 1;
                            } else if (tag.equals(ICON_TAGS)){
                                tagIdentifier = 2;
                            } else if(tag.equals(DESC_TAGS)){
                                tagIdentifier = 3;
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            break;
                        case XmlPullParser.TEXT:
                            if (tagIdentifier == 1) {
                                mInterestTags.put(ID_TAGS, parser.getText().trim());
                            } else if (tagIdentifier == 2) {
                                mInterestTags.put(ICON_TAGS, parser.getText().trim());
                            } else if(tagIdentifier == 3){
                                mInterestTags.put(DESC_TAGS, parser.getText().trim());
                            }
                            Log.d(TAG, "Parser Add : " +  parser.getText());
                            tagIdentifier = 0;
                            break;
                    }
                    parserEvent = parser.next();
                }
            } catch(Exception e){
                Log.d(TAG, e.getMessage());
            }
        }

        Log.d(TAG, "ServiceTypePargser completed, " + Integer.toString(mInterestTags.size()));
    }

    public HashMap<String, String> getResult(){
        return mInterestTags;
    }

    public void run() {
        startParsing();
        if (mHandler != null) {
            mHandler.sendEmptyMessage(0);
        }
    }
}
