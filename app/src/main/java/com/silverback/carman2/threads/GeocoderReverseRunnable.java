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

    static final int GEOCODER_REVERSE_SUCCESS = 1;
    static final int GEOCODER_REVERSE_FAIL = -1;

    // Objects
    private Context context;
    private GeocoderMethods geocoderTask;
    private Location location;

    interface GeocoderMethods {
        Location getLocation();
        void setGeocoderThread(Thread thread);
        void setAddress(String addrs);
        void handleGeocoderReverseTask(int state);
    }

    GeocoderReverseRunnable(Context context, GeocoderMethods methods) {
        this.context = context;
        geocoderTask = methods;
    }

    @Override
    public void run() {
        log.i("GeocoderReverseRunnable");
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        geocoderTask.setGeocoderThread(Thread.currentThread());
        location = geocoderTask.getLocation();

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addressList;


        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 3);
            for(Address addrs : addressList) {
                if(addrs != null) {
                    String address = addrs.getAddressLine(0).substring(5);
                    geocoderTask.setAddress(address);
                    break;
                }
            }

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch(IllegalArgumentException e) {
            log.e("IllegalArgumentException: %s", e.getMessage());
        }


    }
}