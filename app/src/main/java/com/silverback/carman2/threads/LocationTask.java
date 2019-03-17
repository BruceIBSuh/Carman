package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;

import com.google.android.gms.location.LocationCallback;

import java.lang.ref.WeakReference;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Constants
    //private static final String TAG = "LocationTask";

    private WeakReference<View> mWeakView;
    private Context context;
    private Runnable mLocationRunnable;
    private LocationCallback mLocationCallback;
    private Location mLocation;

    // Constructor
    LocationTask(View view) {
        super();
        this.context = view.getContext();
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(ThreadManager threadManager, View view) {

        sThreadManager = threadManager;
        mWeakView = new WeakReference<>(view);
    }

    Runnable getLocationRunnable() {
        return mLocationRunnable;
    }

    void recycle() {
        if(mWeakView != null) {
            mWeakView.clear();
            mWeakView = null;
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

    View getParentView() {
        if(mWeakView != null) return mWeakView.get();
        return null;
    }
}