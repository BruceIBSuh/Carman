package com.silverback.carman.utils;

import android.content.Context;
import android.location.Location;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;

public class CarmanLocationHelper implements
        OnSuccessListener<LocationSettingsResponse>, OnFailureListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanLocationHelper.class);

    // Objects
    //private static CarmanLocationHelper sLocationHelper;
    private Location mLocation;

    private CarmanLocationHelper() {
        // Leave this empty for creating a singleton pattern
        //mLocationRequest = LocationRequest.create();
        //createLocationRequest();
    }


    // Instantiate the singleton class using LazyHolder type.
    private static class CarmanInnerClazz {
        private static final CarmanLocationHelper sLocationInstance = new CarmanLocationHelper();
    }

    public static CarmanLocationHelper getLocationInstance() {
        /*
        if(sLocationHelper == null) {
            sLocationHelper = new CarmanLocationHelper();
        }

        return sLocationHelper;

         */
        return CarmanInnerClazz.sLocationInstance;
    }

    public LocationRequest createLocationRequest() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(Constants.INTERVAL);
        locationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
        locationRequest.setMaxWaitTime(Constants.MAX_WAIT);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return locationRequest;
    }

    public Task<LocationSettingsResponse> createLocationSetting(
            Context context, LocationRequest locationRequest) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

        SettingsClient settingClient = LocationServices.getSettingsClient(context);
        return settingClient.checkLocationSettings(builder.build());
    }


    public LocationCallback initLocationCallback() {

        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for(Location location : locationResult.getLocations())
                    log.i("Locations updated: $s, %s", location, System.currentTimeMillis());
                mLocation = locationResult.getLastLocation();
                log.i("Location in Callback: %s, %s", mLocation.getLatitude(), mLocation.getLongitude());
            }

        };
    }

    public Location getLocation() {
        return mLocation;
    }


    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {}

    @Override
    public void onFailure(@NonNull Exception e){}


}
