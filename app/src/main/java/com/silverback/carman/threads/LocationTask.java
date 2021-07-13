package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.LocationViewModel;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationTask.class);

    // Objects
    private LocationViewModel viewModel;
    private Location mLocation;
    private final Runnable mLocationRunnable;

    // Constructor
    LocationTask(Context context) {
        super();
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(LocationViewModel model) {
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
    }

    @Override
    public void setCurrentLocation(Location location) {
        log.i("current location:%s", location);
        mLocation = location;
        viewModel.getLocation().postValue(mLocation);
    }

    @Override
    public void notifyLocationException(String msg) {
        viewModel.getLocationException().postValue(msg);
    }

    @Override
    public void handleLocationTask(int state) {
        //handleTaskState(this, state);
        int outstate = -1;
        switch(state){
            case LocationRunnable.TASK_COMPLETE:
                outstate = ThreadManager.FETCH_LOCATION_COMPLETED;
                break;
            case LocationRunnable.TASK_FAIL:
                outstate = ThreadManager.FETCH_LOCATION_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}