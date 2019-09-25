package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Looper;
import android.os.Process;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CarmanLocationHelper;

import androidx.annotation.NonNull;

public class LocationRunnable implements Runnable, OnFailureListener, OnSuccessListener<LocationSettingsResponse> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationRunnable.class);

    // Constants
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 1000;
    static final int CURRENT_LOCATION_COMPLETE = 100;
    static final int CURRENT_LOCATION_FAIL = -100;

    // Objects and Fields
    private Context context;
    private static CarmanLocationHelper mLocationHelper;
    private LocationMethods task;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest; //store the Location setting params

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
        this.task = task;
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = mLocationHelper.setLocationRequest();
    }


    @Override
    public void run() {
        task.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // Check if the location setting is successful. If successful, fetch the last known location
        // using FusedLocationProviderClient in onSuccess method.
        mLocationHelper.checkLocationSetting(context)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        log.i("LocationCallback invoked");

        LocationSettingsStates locationStates = locationSettingsResponse.getLocationSettingsStates();
        if(locationStates.isGpsUsable()) log.i("GPS is working");
        if(locationStates.isNetworkLocationUsable()) log.i("Network location is working");

        // LocationCallback should be initiated as long as LocationSettingsRequest has been
        // successfully accepted.
        LocationCallback locationCallback = mLocationHelper.initLocationCallback();
        //Location mLocation = mLocationHelper.getLocation();

        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if(location != null) {
                    log.e("Current Location: %s", location);
                    task.setCurrentLocation(location);
                    task.handleLocationTask(CURRENT_LOCATION_COMPLETE);
                } else {
                    task.handleLocationTask(CURRENT_LOCATION_FAIL);
                }
            });

        } catch (SecurityException e) {
            log.e("Location_SecurityException: %s", e.getMessage());

        } finally {
            log.e("Location finished");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }




    }

    @Override
    public void onFailure(@NonNull Exception e) {

        if(e instanceof ResolvableApiException) {
            // Location Settings are not satisfied, but this can be fixed by showing the user a dialog
            try {
                log.e("ResolvableApiException");
                // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult
                ResolvableApiException resolvable = (ResolvableApiException) e;
                resolvable.startResolutionForResult((Activity)context, REQUEST_CHECK_LOCATION_SETTINGS);
                ThreadManager.cancelAllThreads();

            } catch (IntentSender.SendIntentException sendEx) {
                // Ignore the error
                log.e("LocationSettings exceiption: %s", e.getMessage());
            }
        }
    }

}

