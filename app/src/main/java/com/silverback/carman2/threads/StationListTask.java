package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.views.StationRecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class StationListTask extends ThreadTask implements
        StationListRunnable.StationListMethod,
        FireStoreSetRunnable.FireStoreSetMethods, FireStoreGetRunnable.FireStoreGetMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListTask.class);

    // Objects
    private Context context;
    private WeakReference<StationRecyclerView> mWeakRecyclerView;
    private Runnable mStationListRunnable;
    private Runnable mFireStoreSetRunnable;
    private Runnable mFireStoreGetRunnable;
    private List<Opinet.GasStnParcelable> mStationList; //used by StationListRunnable

    private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    private Location mLocation;
    private String[] defaultParams;
    private int count = 0;

    private static ThreadManager sThreadManager;

    // Constructor
    StationListTask(Context context) {
        super();
        this.context = context;
        mStationListRunnable = new StationListRunnable(context, this);
        mFireStoreSetRunnable = new FireStoreSetRunnable(this);
        mFireStoreGetRunnable = new FireStoreGetRunnable(this);
    }

    void initStationTask(
            ThreadManager threadManager, StationRecyclerView view, String[] params, Location location) {

        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
        mWeakRecyclerView = new WeakReference<>(view);
        mStationInfoList = new ArrayList<>();
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    Runnable getFireStoreSetRunnalbe() { return mFireStoreSetRunnable; }
    Runnable getFireStoreGetRunnable() { return mFireStoreGetRunnable; }

    void recycle() {
        if(mWeakRecyclerView != null) {
            mWeakRecyclerView.clear();
            mWeakRecyclerView = null;
        }

        mStationList = null;
    }

    // Callback invoked by StationListRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public synchronized void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
        log.i("Download Thread: %s", thread);
    }

    // The following 3 callbacks are invoked by StationListRunnable to retrieve stations within
    // a radius and location, then give them back by setStationList().
    @Override
    public String[] getDefaultParam() {
        return defaultParams;
    }

    @Override
    public Location getStationLocation() {
        return mLocation;
    }


    @Override
    public void setStationList(List<Opinet.GasStnParcelable> list) {
        mStationList = list;
    }

    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch (state) {
            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_COMPLETE:
                log.i("DOWNLOAD_NEAR_STATIONS_COMPLETE");
                outState = ThreadManager.DOWNLOAD_STATION_LIST_COMPLETED;
                break;

            case StationListRunnable.DOWNLAOD_CURRENT_STATION_COMPLETE:
                break;

            case StationListRunnable.DOWNLOAD_CURRENT_STATION_FAILED:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETE;
                break;

            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_FAILED:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }

}