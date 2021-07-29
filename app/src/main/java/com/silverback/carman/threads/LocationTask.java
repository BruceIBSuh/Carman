package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

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

    void initLocationTask(LocationViewModel viewModel) {
        this.viewModel = viewModel;
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
        viewModel.getLocation().postValue(location);
    }

    @Override
    public void notifyLocationException(String msg) {
        viewModel.getLocationException().postValue(msg);
    }

    @Override
    public void handleLocationTask(int state) {
        int outstate = -1;
        switch(state){
            case LocationRunnable.LOCATION_TASK_COMPLETE:
                outstate = ThreadManager2.FETCH_LOCATION_COMPLETED;
                break;
            case LocationRunnable.LOCATION_TASK_FAIL:
                outstate = ThreadManager2.FETCH_LOCATION_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}