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

public class GeocoderRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeocoderRunnable.class);

    private Context context;
    private GeocoderMethods geocoderTask;
    private Location geocoderLocation;

    interface GeocoderMethods {
        void setGeocoderThread(Thread thread);
        void setGeocoderLocation(Location location);
        String getGeocoderAddress();
    }

    GeocoderRunnable(Context context, GeocoderMethods task) {
       this.context = context;
       geocoderTask = task;
       geocoderLocation = new Location("geocoderLocation");
    }


    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        geocoderTask.setGeocoderThread(Thread.currentThread());

        String locationName = geocoderTask.getGeocoderAddress();
        log.i("Location Name: %s", locationName);

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addrsList = geocoder.getFromLocationName(locationName, 2);
            log.i("Geocoder addrs List: %s", addrsList.size());
            for(Address addrs : addrsList) {
                log.i("Geocoder Address: %s", addrs);
                if(addrs.hasLatitude() && addrs.hasLongitude()) {
                    geocoderLocation.setLatitude(addrs.getLatitude());
                    geocoderLocation.setLongitude(addrs.getLongitude());
                }

                log.i("Geocoder Location: %s", geocoderLocation);
                geocoderTask.setGeocoderLocation(geocoderLocation);

            }
        } catch(IOException e) {
            log.e("Geocoder IOException: %s", e.getMessage());
        }



    }
}
