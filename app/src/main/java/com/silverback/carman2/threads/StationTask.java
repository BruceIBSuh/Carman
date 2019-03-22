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
    private Context context;
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    //private WeakReference<StationListView> mWeakListView;
    //private WeakReference<GasManagerActivity> mWeakActivity;
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    private Runnable mStationListRunnable;
    private Runnable mStationInfoRunnable;
    private List<Opinet.GasStnParcelable> mStationList, mInformedStationList;
    private Location mLocation;
    private String[] defaultParams;
    private String stationId;

    // Constructor
    StationTask(Context context) {
        super();
        mStationListRunnable = new StationListRunnable(context, this);
        mStationInfoRunnable = new StationInfoRunnable(this);
    }

    void initStationTask(ThreadManager threadManager, String[] params, Location location) {
        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
        mInformedStationList = new ArrayList<>();

    }

    /*
    public void initSortTask(ThreadManager threadManager,
                             //OpinetStationListFragment fm,
                             StationListView listView,
                             List<Opinet.GasStnParcelable> list) {

        sThreadManager = threadManager;
        mWeakFragment = new WeakReference<>(fm);
        mWeakListView = new WeakReference<>(listView);
        mStationList = list;
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

        mStationList = null;
        mInformedStationList = null;
        defaultParams = null;

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
        mStationList = list;
    }

    // Call back by StationInfoRunnable to add additional station information.
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    // Callback by StationInfoRunnable to get back Opinet.GasStnParcelable modified with
    // adding additional information(e.g. Car wahs here), then add it to a new List.
    @Override
    public void addGasStationInfo(Opinet.GasStnParcelable parcelable) {
        log.i("GasStnParcelable with Car Wash: %s, %s", parcelable.getIsCarWash(), parcelable.getStnName());
        mInformedStationList.add(parcelable);
    }

    @Override
    public void handleStationTaskState(int state) {
        log.i("handlestate");
        int outState = -1;

        switch (state) {
            case StationListRunnable.STATION_LIST_COMPLETE:
                outState = ThreadManager.STATIONTASK_LIST_COMPLETED;
                break;

            case StationInfoRunnable.STATION_INFO_COMPLETE:
                log.i("Opinet.GasStationParcelable:");

                outState= ThreadManager.STATIONTASK_INFO_COMPLETE;
                break;

            case StationListRunnable.STATION_CURRENT_FAIL:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETED;
                break;

            case StationListRunnable.STATION_LIST_FAIL:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }

    List<Opinet.GasStnParcelable> getInformedStationList() {
        return mInformedStationList;
    }
}