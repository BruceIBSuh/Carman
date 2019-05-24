package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;
import android.view.View;

import com.google.android.gms.location.LocationCallback;

import java.lang.ref.WeakReference;

public class LocationTask extends ThreadTask implements LocationRunnable.LocationMethods {

    // Constants
    //private static final String TAG = "LocationTask";

    //private WeakReference<View> mWeakView;
    //private Context context;
    private Runnable mLocationRunnable;
    private Location mLocation;

    // Constructor
    LocationTask(Context context) {
        super();
        //this.context = context;
        mLocationRunnable = new LocationRunnable(context, this);
    }

    void initLocationTask(ThreadManager threadManager) {
        sThreadManager = threadManager;
    }

    Runnable getLocationRunnable() {
        return mLocationRunnable;
    }

    void recycle() {}

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

    // Invoked by hadleMessage(Message) in ThreadManager to pass the current location over to
    // the UI Thread in GeneralFragment or GasManagerFragment.
    Location getLocationUpdated() {
        return mLocation;
    }
}