package com.AutoIDLabs.KAIST.GS1Beacon;

import android.graphics.Bitmap;

/**
 * Created by SNAIL on 2016-02-14.
 */
public class ServiceItem {

    private String FQDN;         //GS1 code from Beacon.
    private String serviceURL;     //ServiceURL from NAPTR.
    private String serviceType;    //ServiceType from xml.
    private Bitmap serviceIcon;    //SeviceIcon from xml.
    private String ab;                 //Service Abstract from xml.
    private boolean favorite;       //Service filter.

    //Creator
    public ServiceItem(String code, String url, String type, Bitmap icon, String a) {
        this.FQDN = code;
        this.serviceURL = url;
        this.serviceType = type;
        this.serviceIcon = icon;
        this.ab = a;
        this.favorite = true;
    }

    public String getFQDN() {
        return FQDN;
    }

    public void setFQDN(String FQDN) {
        this.FQDN = FQDN;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
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

    public String getAb() {
        return ab;
    }

    public void setAb(String ab) {
        this.ab = ab;
    }

    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Override
    public String toString()
    {
        return FQDN + " | " + serviceURL + " | " + serviceType + " | " + serviceIcon + " | " + ab + " | " + favorite;
    }

}
