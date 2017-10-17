package com.AutoIDLabs.KAIST.GS1Beacon;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.util.ArrayList;

/**
 * Created by SNAIL on 2016-02-15.
 */
public class ApplicationClass extends Application {

    public ArrayList<ServiceItem> availableService = new ArrayList<>();

    /*
    public static final Integer[] icon =
            { R.drawable.item, R.drawable.location, R.drawable.price, R.drawable.coupon,
                    R.drawable.payment, R.drawable.auth, R.drawable.info, R.drawable.iden,
                    R.drawable.service};
                    */

    @Override
    public void onCreate() {

        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.item);
        Bitmap itemIcon = drawable.getBitmap();

        ServiceItem item = new ServiceItem("", "", "urn:autoidlabsk:sts:global:item", itemIcon, "1");
        ServiceItem location = new ServiceItem("", "", "urn:autoidlabsk:sts:global:location", itemIcon, "1");
        ServiceItem notification = new ServiceItem("", "", "urn:autoidlabsk:sts:global:price", itemIcon, "1");
        ServiceItem coupon = new ServiceItem("", "", "urn:autoidlabsk:sts:global:coupon", itemIcon, "1");
        ServiceItem paymnet = new ServiceItem("", "", "urn:autoidlabsk:sts:global:payment", itemIcon, "1");
        ServiceItem authen = new ServiceItem("", "", "urn:autoidlabsk:sts:global:authentication", itemIcon, "1");
        ServiceItem author = new ServiceItem("", "", "urn:autoidlabsk:sts:global:information", itemIcon, "1");
        ServiceItem id = new ServiceItem("", "", "urn:autoidlabsk:sts:global:member", itemIcon, "1");
        ServiceItem serviceRelation = new ServiceItem("", "", "urn:autoidlabsk:sts:global:servicerelation", itemIcon, "1");
        ServiceItem service = new ServiceItem("", "", "urn:autoidlabsk:sts:global:service", itemIcon, "1");
        ServiceItem koreannet = new ServiceItem("", "", "urn:autoidlabsk:sts:global:koreannet", itemIcon, "1");
        ServiceItem google = new ServiceItem("", "", "urn:autoidlabsk:sts:global:google", itemIcon, "1");
        ServiceItem lotte = new ServiceItem("", "", "urn:autoidlabsk:sts:private:lotte:item", itemIcon, "1");
        ServiceItem pedigree = new ServiceItem("", "", "urn:autoidlabsk:sts:global:pedigree", itemIcon, "1");
        ServiceItem car = new ServiceItem("", "", "urn:autoidlabsk:sts:global:car", itemIcon, "1");
        ServiceItem bus = new ServiceItem("", "", "urn:autoidlabsk:sts:global:bus", itemIcon, "1");

        availableService.add(item);
        availableService.add(location);
        availableService.add(notification);
        availableService.add(coupon);
        availableService.add(paymnet);
        availableService.add(authen);
        availableService.add(author);
        availableService.add(id);
        availableService.add(serviceRelation);
        availableService.add(service);
        availableService.add(koreannet);
        availableService.add(google);
        availableService.add(lotte);
        availableService.add(pedigree);
        availableService.add(car);
        availableService.add(bus);
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public Boolean getCheckByType(String type) {
        for (int i = 0; i < availableService.size(); i++) {
            if(type.equals(availableService.get(i).getServiceType()))
                return availableService.get(i).getFavorite();
        }
        return false;
    }

    public ArrayList<String> getTypes () {
        ArrayList<String> ret = new ArrayList<>();

        for (int i =0; i < availableService.size(); i++) {
            ret.add(availableService.get(i).getServiceType());
        }

        return ret;
    }

}
