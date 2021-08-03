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
import com.google.android.gms.location.LocationResult;
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

public class LocationRunnable implements
        Runnable, OnFailureListener, OnSuccessListener<LocationSettingsResponse> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationRunnable.class);

    // Constants
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 1000;
    static final int LOCATION_TASK_COMPLETE = 1;
    static final int LOCATION_TASK_FAIL= -1;

    // Objects and Fields
    private final Context context;
    private final LocationMethods task;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest; //store the Location setting params
    private LocationCallback locationCallback;
    private Location mCurrentLocation;


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


    }

    @Override
    public void run() {
        task.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = CarmanLocationHelper.getLocationInstance().setLocationRequest();

        // LocationCallback should be initiated as long as LocationSettingsRequest has been
        // successfully accepted.
        CarmanLocationHelper.getLocationInstance().checkLocationSetting(context)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for(Location location : locationResult.getLocations())
                    log.i("Locations updated: $s, %s", location, System.currentTimeMillis());
                mCurrentLocation = locationResult.getLastLocation();
                task.setCurrentLocation(mCurrentLocation);
                task.handleLocationTask(LOCATION_TASK_COMPLETE);
                mFusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };

    }

    // Check if the Location setting is successful using CarmanLocationHelper
    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        log.d("location setting");
        LocationSettingsStates locationStates = locationSettingsResponse.getLocationSettingsStates();
        if(locationStates != null && !locationStates.isGpsUsable()) {
            log.i("GPS is not working");
            task.notifyLocationException(context.getString(R.string.location_notify_gps));
        } else if(locationStates != null && !locationStates.isNetworkLocationUsable()) {
            log.i("Network location is not working");
            task.notifyLocationException(context.getString(R.string.location_notify_network));
        } else {

            try {
               mFusedLocationClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.getMainLooper());

               /*
               mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                   if (location != null) {
                        log.i("location fetched:%s", location);
                        mCurrentLocation = location;
                        task.setCurrentLocation(location);
                        task.handleLocationTask(LOCATION_TASK_COMPLETE);
                   } else {
                        log.i("location null");
                        mFusedLocationClient.requestLocationUpdates(
                                locationRequest, locationCallback, Looper.getMainLooper());
                   }
               });

                */

            } catch (SecurityException e) {
                log.e("Location_SecurityException: %s", e.getMessage());
                task.notifyLocationException(context.getString(R.string.location_exception_security));
                task.handleLocationTask(LOCATION_TASK_FAIL);
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {

        if (e instanceof ResolvableApiException) {
            // Location Settings are not satisfied, but this can be fixed by showing the user a dialog
            // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult
            try {
                ResolvableApiException resolvable = (ResolvableApiException) e;
                resolvable.startResolutionForResult((Activity) context, REQUEST_CHECK_LOCATION_SETTINGS);
            } catch (IntentSender.SendIntentException sendEx) {
                e.printStackTrace();
            }

            task.handleLocationTask(LOCATION_TASK_FAIL);
        }
    }
}

