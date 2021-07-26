package com.silverback.carman.threads;

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
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.CarmanLocationHelper;

import androidx.annotation.NonNull;

public class LocationRunnable implements Runnable, OnFailureListener, OnSuccessListener<LocationSettingsResponse> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationRunnable.class);

    // Constants
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 1000;
    //static final int CURRENT_LOCATION_COMPLETE = 1;
    //static final int CURRENT_LOCATION_FAIL = -1;

    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL= -1;

    // Objects and Fields
    private final Context context;
    private static final CarmanLocationHelper mLocationHelper;
    private final LocationMethods task;
    private final FusedLocationProviderClient mFusedLocationClient;
    private final LocationRequest locationRequest; //store the Location setting params

    static {
        mLocationHelper = CarmanLocationHelper.getLocationInstance();
    }

    // Interface
    public interface LocationMethods {
        void setDownloadThread(Thread thread);
        void setCurrentLocation(Location location);
        void notifyLocationException(String msg);
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

    // Check if the Location setting is successful using CarmanLocationHelper
    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

        LocationSettingsStates locationStates = locationSettingsResponse.getLocationSettingsStates();
        if(locationStates != null && !locationStates.isGpsUsable()) {
            log.i("GPS is not working");
            task.notifyLocationException(context.getString(R.string.location_notify_gps));
        } else if(locationStates != null && !locationStates.isNetworkLocationUsable()) {
            log.i("Network location is not working");
            task.notifyLocationException(context.getString(R.string.location_notify_network));
        }

        // LocationCallback should be initiated as long as LocationSettingsRequest has been
        // successfully accepted.
        LocationCallback locationCallback = mLocationHelper.initLocationCallback();
        //Location mLocation = mLocationHelper.getLocation();

        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if(location != null) {
                    log.i("location fetched:%s", location);
                    //TEMP CODE FOR TESTING
                    /*
                    location.setLongitude(126.8991);
                    location.setLatitude(37.5145);
                    */
                    task.setCurrentLocation(location);
                    task.handleLocationTask(TASK_COMPLETE);

                } else {
                    task.notifyLocationException(context.getString(R.string.location_null));
                    task.handleLocationTask(TASK_FAIL);
                }
            });

        } catch (SecurityException e) {
            log.e("Location_SecurityException: %s", e.getMessage());
            task.notifyLocationException(context.getString(R.string.location_exception_security));
            task.handleLocationTask(TASK_FAIL);

        } finally {
            log.e("Location finished");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {

        if (e instanceof ResolvableApiException) {
            // Location Settings are not satisfied, but this can be fixed by showing the user a dialog
            try {
                // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult
                ResolvableApiException resolvable = (ResolvableApiException) e;
                resolvable.startResolutionForResult((Activity) context, REQUEST_CHECK_LOCATION_SETTINGS);
            } catch (IntentSender.SendIntentException sendEx) {
                e.printStackTrace();
            }

            task.handleLocationTask(TASK_FAIL);
        }
    }
}

