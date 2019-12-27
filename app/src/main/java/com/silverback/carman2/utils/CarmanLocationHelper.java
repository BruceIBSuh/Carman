package com.silverback.carman2.utils;

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
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;

public class CarmanLocationHelper implements
        OnSuccessListener<LocationSettingsResponse>, OnFailureListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanLocationHelper.class);

    // Objects
    private static CarmanLocationHelper sLocationHelper;
    private LocationRequest mLocationRequest;
    private Location mLocation;


    // private Constructor
    private CarmanLocationHelper() {
        // Leave this empty for creating a singleton pattern
        mLocationRequest = LocationRequest.create();
        setLocationRequest();
    }

    // Singleton for instantiating this.
    public static CarmanLocationHelper getLocationInstance() {
        if(sLocationHelper == null) {
            sLocationHelper = new CarmanLocationHelper();
        }

        return sLocationHelper;
    }

    //private LocationSettingsRequest setLocationRequest() {
    public LocationRequest setLocationRequest() {

        mLocationRequest.setInterval(Constants.INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
        mLocationRequest.setMaxWaitTime(Constants.MAX_WAIT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return mLocationRequest;
    }

    public Task<LocationSettingsResponse> checkLocationSetting(final Context context) {
        //Log.i(LOG_TAG, "Check Location setting");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);

        SettingsClient mSettingsClient = LocationServices.getSettingsClient(context);
        return mSettingsClient.checkLocationSettings(builder.build());
        //.addOnSuccessListener(this)
        //.addOnFailureListener(this);
    }


    public LocationCallback initLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if(locationResult == null) return;

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
