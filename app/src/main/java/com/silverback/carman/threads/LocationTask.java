package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.LocationViewModel;

import java.lang.ref.WeakReference;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LocationTask.class);

    // Objects
    private LocationViewModel viewModel;
    //private WeakReference<LocationViewModel> weakModelReference;
    private Location mLocation;
    private final Runnable mLocationRunnable;

    // Constructor
    LocationTask(Context context) {
        super();
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(LocationViewModel viewModel) {
        this.viewModel = viewModel;
        //weakModelReference = new WeakReference<>(viewModel);
    }

    Runnable getLocationRunnable() {
        return mLocationRunnable;
    }

    public void recycle() {
        if(mLocation != null) mLocation = null;
        /*
        if(weakModelReference != null) {
            weakModelReference.clear();
            weakModelReference = null;
        }

         */
    }

    @Override
    public void setLocationThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setCurrentLocation(Location location) {
        log.i("current location:%s", location);
        mLocation = location;
        //weakModelReference.get().getLocation().postValue(location);
        viewModel.getLocation().postValue(location);
    }

    @Override
    public void notifyLocationException(String msg) {
        //weakModelReference.get().getLocationException().postValue(msg);
        viewModel.getLocationException().postValue(msg);
    }

    @Override
    public void handleLocationTask(int state) {
        int outstate = -1;
        switch(state){
            case LocationRunnable.LOCATION_TASK_COMPLETE:
                outstate = sThreadManager.TASK_COMPLETE;
                break;
            case LocationRunnable.LOCATION_TASK_FAIL:
                outstate = sThreadManager.TASK_FAIL;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}