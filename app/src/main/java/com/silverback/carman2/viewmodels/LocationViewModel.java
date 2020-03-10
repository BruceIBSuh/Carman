package com.silverback.carman2.viewmodels;

import android.location.Location;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class LocationViewModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationViewModel.class);

    // Objects
    private MutableLiveData<Location> location;
    private MutableLiveData<String> address;
    private MutableLiveData<Location> geocoderLocation;
    private MutableLiveData<String> locationException;

    public MutableLiveData<Location> getLocation() {
        if(location == null) {
            location = new MutableLiveData<>();
        }

        return location;
    }

    public MutableLiveData<String> getLocationException() {
        if(locationException == null) locationException = new MutableLiveData<>();
        return locationException;
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
