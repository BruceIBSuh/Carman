package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocoderReverseRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeocoderReverseRunnable.class);

    // Objects
    private Context context;
    private GeocoderMethods geocoderTask;
    private Location location;

    interface GeocoderMethods {
        void setGeocoderThread(Thread thread);
        void setAddress(String addrs);
        Location getLocation();

    }

    GeocoderReverseRunnable(Context context, GeocoderMethods methods) {
        this.context = context;
        geocoderTask = methods;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        geocoderTask.setGeocoderThread(Thread.currentThread());
        location = geocoderTask.getLocation();

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addressList = null;

        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch(IllegalArgumentException e) {
            log.e("IllegalArgumentException: %s", e.getMessage());
        }

        if(addressList != null && addressList.size() > 0) {
            log.i("Address: %s", addressList.get(0));
            String address = addressList.get(0).getAddressLine(0).substring(5);
            geocoderTask.setAddress(address);
        }
    }
}
