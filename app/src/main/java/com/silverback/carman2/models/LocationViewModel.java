package com.silverback.carman2.models;

import android.location.Address;
import android.location.Location;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.reflect.Array;

public class LocationViewModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationViewModel.class);

    // Objects
    private MutableLiveData<Location> location;
    private MutableLiveData<String> address;
    private MutableLiveData<Location> geocoderLocation;

    public MutableLiveData<Location> getLocation() {
        if(location == null) {
            location = new MutableLiveData<>();
        }

        return location;
    }

    public MutableLiveData<String> getAddress() {
        if(address == null) address = new MutableLiveData<>();
        return address;
    }

    public MutableLiveData<Location> getGeocoderLocation() {
        if(geocoderLocation == null) geocoderLocation = new MutableLiveData<>();
        return geocoderLocation;
    }

}
