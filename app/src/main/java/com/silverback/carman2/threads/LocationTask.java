package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationTask.class);

    // Objects
    private LocationViewModel viewModel;
    private Location mLocation;
    private Runnable mLocationRunnable;

    // Constructor
    LocationTask(Context context) {
        super();
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(LocationViewModel model) {
        //sThreadManager = threadManager;
        viewModel = model;
    }

    Runnable getLocationRunnable() {
        return mLocationRunnable;
    }

    void recycle() {
        if(mLocation != null) mLocation = null;
    }

    @Override
    public void setDownloadThread(Thread thread) {
        setCurrentThread(thread);
        //Log.i(TAG, "Location Thread: " + thread);
    }

    @Override
    public void setCurrentLocation(Location location) {
        log.i("Update Location from worker thread");
        mLocation = location;
        viewModel.getLocation().setValue(mLocation);
    }

    @Override
    public void handleLocationTask(int state) {

        int outstate = -1;

        switch(state){
            case LocationRunnable.CURRENT_LOCATION_COMPLETE:
                outstate = ThreadManager.FETCH_LOCATION_COMPLETED;
                break;
            case LocationRunnable.CURRENT_LOCATION_FAIL:
                outstate = ThreadManager.FETCH_LOCATION_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }
}