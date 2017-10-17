package com.AutoIDLabs.KAIST.GS1Beacon;

import android.graphics.Bitmap;

/**
 * Created by SNAIL on 2016-03-10.
 */
public class CacheItem {

    private String serviceType;    //ServiceType from xml.
    private Bitmap serviceIcon;                //ServiceIcon.png

    //Creator
    public CacheItem(String xml, Bitmap icon) {
        this.serviceType = xml;
        this.serviceIcon = icon;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Bitmap getServiceIcon() {
        return serviceIcon;
    }

    public void setServiceIcon(Bitmap serviceIcon) {
        this.serviceIcon = serviceIcon;
    }

    @Override
    public String toString()
    {
        return serviceType + " | " + serviceIcon;
    }

}
