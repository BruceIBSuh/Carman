package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationCallback;

import java.lang.ref.WeakReference;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Constants
    //private static final String TAG = "LocationTask";

    private WeakReference<Context> mWeakContext;
    private Runnable mLocationRunnable;
    private LocationCallback mLocationCallback;
    private Location mLocation;

    // Constructor
    LocationTask(Context context) {
        super();
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(ThreadManager threadManager, Context context) {

        sThreadManager = threadManager;
        mWeakContext = new WeakReference<>(context);
    }

    Runnable getLocationRunnable() {
        return mLocationRunnable;
    }

    public void recycle() {
        if(mWeakContext != null) {
            mWeakContext.clear();
            mWeakContext = null;
        }

        if(mLocationCallback != null) mLocationCallback = null;
    }

    @Override
    public void setDownloadThread(Thread thread) {
        setCurrentThread(thread);
        //Log.i(TAG, "Location Thread: " + thread);
    }

    @Override
    public void setCurrentLocation(Location location) {
        mLocation = location;
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

    Location getLocationUpdated() {
        return mLocation;
    }

    Activity getParentActivity() {
        if(mWeakContext != null) return (Activity)mWeakContext.get();
        return null;

    }


}