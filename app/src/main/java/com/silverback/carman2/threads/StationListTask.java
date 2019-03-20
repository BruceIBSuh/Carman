package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.List;

public class StationListTask extends ThreadTask implements StationListRunnable.StationListMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListTask.class);

    // Objects
    private Context context;
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    //private WeakReference<StationListView> mWeakListView;
    //private WeakReference<GasManagerActivity> mWeakActivity;
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    private Runnable mStationListRunnable;
    private List<Opinet.GasStnParcelable> mStationList;
    private Location mLocation;
    private String[] defaultParams;

    // Constructor
    StationListTask(Context context) {
        super();
        mStationListRunnable = new StationListRunnable(context, this);
    }

    void initDownloadListTask(ThreadManager threadManager, String[] params, Location location) {
        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
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
        defaultParams = null;

    }

    // StationListRunnable.StationsLoadMethod

    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
        log.i("Download Thread: %s", thread);
    }

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
    public void handleStationTaskState(int state) {
        log.i("handlestate");
        int outState = -1;

        switch (state) {
            case StationListRunnable.DOWNLOAD_STATION_LIST_COMPLETE:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_COMPLETED;
                break;

            case StationListRunnable.DOWNLOAD_NO_NEAR_STATION:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETED;
                break;

            case StationListRunnable.DONWLOAD_STATION_LIST_FAIL:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }


    List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }
}