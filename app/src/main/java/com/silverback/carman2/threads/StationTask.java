package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.ArrayList;
import java.util.List;

public class StationTask extends ThreadTask implements
        StationListRunnable.StationListMethod, StationInfoRunnable.StationInfoMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationTask.class);

    // Objects
    private Runnable mStationListRunnable;
    private Runnable mStationInfoRunnable;
    private List<Opinet.GasStnParcelable> stationList;



    private List<Opinet.GasStnParcelable> mStationInfoList;
    private Location mLocation;
    private String[] defaultParams;
    private Opinet.GasStnParcelable station;

    // Fields
    private int count;

    // Constructor
    StationTask(Context context) {
        super();
        mStationInfoList = new ArrayList<>();
        mStationListRunnable = new StationListRunnable(context, this);
        mStationInfoRunnable = new StationInfoRunnable(context, this);
    }

    void initStationTask(ThreadManager threadManager, String[] params, Location location) {
        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
        //mInformedStationList = new ArrayList<>();
    }

    /*
    //Long monitor contention with owner pool-1-thread-2 (15944)
    //at void com.silverback.carman2.threads.StationInfoRunnable.run()(StationInfoRunnable.java:-1)

    void initStationInfo(Opinet.GasStnParcelable station) {
        this.station = station;
    }
    */


    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    Runnable getStationInfoRunnalbe() { return mStationInfoRunnable; }


    void recycle() {

        /*
        // Deletes the weak reference to the imageView
        if (mWeakFragment != null) {
            mWeakFragment.clear();
            mWeakFragment = null;
        }

        if (mWeakListView != null) {
            mWeakListView.clear();
            mWeakListView = null;
        }
        */

        //mStationList = null;
        //defaultParams = null;

    }

    // Callback invoked by StationListRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public void setStationTaskThread(Thread thread) {
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
        stationList = list;
    }

    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return stationList;
    }

    @Override
    public void addStationInfo(Opinet.GasStnParcelable station) {
        mStationInfoList.add(station);
    }



    // Check if all the StationInfoRunnables are complete compared with the count that is equal to
    // the size of the StationList.

    /*
    @Override
    public void addCount() {
        count++;
    }

    @Override
    public int getStationIndex() {
        return count;
    }


    @Override
    public Opinet.GasStnParcelable getStation() {
        return station;
    }


    */




    // Callback by StationInfoRunnable to get back Opinet.GasStnParcelable modified with
    // adding additional information(e.g. Car wahs here), then add it to a new List.
    /*
    @Override
    public void initStationInfo(Opinet.GasStnParcelable parcelable) {
        mInformedStationList.add(parcelable);
    }
    */

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch (state) {
            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STATION_LIST_COMPLETE;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_COMPLETE:
                log.i("Opinet.GasStationParcelable:");
                outState= ThreadManager.DOWNLOAD_STATION_INFO_COMPLETE;
                break;

            case StationListRunnable.DOWNLOAD_CURRENT_STATION_FAILED:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETE;
                break;

            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_FAILED:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_FAILED:
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }



    List<Opinet.GasStnParcelable> getStationInfoList() {
        //return mInformedStationList;
        return mStationInfoList;
    }
}