package com.silverback.carman2.models;

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

    public MutableLiveData<Location> getLocation() {
        if(location == null) {
            location = new MutableLiveData<>();
        }
        log.i("Location in ViewModel: %s", location);
        return location;
    }

}
