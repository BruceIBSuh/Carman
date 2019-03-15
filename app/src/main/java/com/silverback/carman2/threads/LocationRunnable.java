package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.CarmanLocationHelper;

public class LocationRunnable implements Runnable,
        OnFailureListener, OnSuccessListener<LocationSettingsResponse> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationRunnable.class);

    // Constants
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 1000;
    static final int CURRENT_LOCATION_COMPLETE = 100;
    static final int CURRENT_LOCATION_FAIL = -100;

    // Objects and Fields
    private Context context;
    private static CarmanLocationHelper mLocationHelper;
    private LocationMethods mLocationTask;

    static {
        mLocationHelper = CarmanLocationHelper.getLocationInstance();
    }

    // Interface
    public interface LocationMethods {
        void setDownloadThread(Thread thread);
        void setCurrentLocation(Location location);
        void handleLocationTask(int state);
    }

    // Constructor
    LocationRunnable(Context context, LocationMethods task) {
        mLocationTask = task;
        this.context = context;
    }


    @Override
    public void run() {

        log.i("LocationRunnable");

        mLocationTask.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // Flag to tell whether the location updates are permitted.
        FusedLocationProviderClient mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context);

        // Set the Location environment variables and check if it is appropriatte, implementing
        // OnSuccessListener and OnFailureLister, the methods of which are defined in the bottom.
        LocationRequest locationRequest = mLocationHelper.setLocationRequest();
        mLocationHelper.checkLocationSetting(context)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);


        // LocationCallback should be initiated as long as LocationSettingsRequest has been
        // successfully accepted.
        LocationCallback locationCallback = mLocationHelper.initLocationCallback();
        //Location mLocation = mLocationHelper.getLocation();

        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // getLoastLocation() Invokes LocationCallback.onLocationChanged(), then get a updated location
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null) {
                        log.i("Location: %s, %s", location.getLongitude(), location.getLatitude());
                        mLocationTask.setCurrentLocation(location);
                        mLocationTask.handleLocationTask(CURRENT_LOCATION_COMPLETE);

                    } else {
                        mLocationTask.handleLocationTask(CURRENT_LOCATION_FAIL);
                    }
                }
            });


        } catch (SecurityException e) {
            e.printStackTrace();

        } finally {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

        //LocationSettingsStates locationStates = locationSettingsResponse.getLocationSettingsStates();
        /*
        if(locationStates.isGpsUsable()) {
            //Log.d(LOG_TAG, "GPS is working");
        }
        if(locationStates.isNetworkLocationUsable()) {
            //Log.d(LOG_TAG, "Network location is working");
        }
        */

    }

    @Override
    public void onFailure(@NonNull Exception e) {

        if(e instanceof ResolvableApiException) {
            // Location Settings are not satisfied, but this can be fixed by showing the user a dialog
            try {
                //Log.e(LOG_TAG, "ResolvableApiException");
                // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult
                ResolvableApiException resolvable = (ResolvableApiException) e;
                resolvable.startResolutionForResult((Activity)context, REQUEST_CHECK_LOCATION_SETTINGS);
                ThreadManager.cancelAllThreads();

            } catch (IntentSender.SendIntentException sendEx) {
                // Ignore the error
                //Log.e(TAG, "LocationSettings exceiption: " + e.getMessage());
            }
        }
    }

}

