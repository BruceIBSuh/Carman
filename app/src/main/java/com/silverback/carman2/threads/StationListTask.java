package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;
import android.support.v4.app.Fragment;

import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.List;

public class StationListTask extends ThreadTask implements
        StationDownloadRunnable.StationsDownloadMethod,
        StationListRunnable.StationListMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListTask.class);

    // Objects
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    //private WeakReference<StationListView> mWeakListView;
    //private WeakReference<GasManagerActivity> mWeakActivity;
    //private WeakReference<OpinetStationListFragment> mWeakFragment;
    private Runnable mDownloadRunnable;
    private Runnable mListRunnable;
    //private Runnable mInfoRunnable;
    //private StationListAdapter mAdapter;
    private List<Opinet.GasStnParcelable> mStationList;
    private Location mLocation;
    private String[] defaultParams;
    //private String mStationAddrs;
    //private String mStationCode;

    // Constructor
    StationListTask(Context context) {
        super();
        mDownloadRunnable = new StationDownloadRunnable(context, this);
        mListRunnable = new StationListRunnable(this);
        //mAdapter = new StationListAdapter(context);
    }

    void initDownloadTask(ThreadManager threadManager, Fragment fm, String[] params, Location location) {

        sThreadManager = threadManager;
        //mWeakListView = new WeakReference<>(recyclerView);
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
    Runnable getStationDownloadRunnable() {
        return mDownloadRunnable;
    }


    Runnable getStationInfoRunnable() {
        return mListRunnable;
    }

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


    // StationListRunnable.StationListMethods invokes

    @Override
    public void setStationListThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }


    @Override
    public StationListAdapter getStationListAdapter() {
        return null;//mAdapter;
    }


    // StationDownloadRunnable.StationsLoadMethod

    @Override
    public void setDownloadThread(Thread thread) {
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
    public void handleDownloadTaskState(int state) {
        //Log.i(TAG, "handlestate");
        int outState = -1;

        switch (state) {
            case StationDownloadRunnable.DOWNLOAD_STATION_LIST_COMPLETE:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_COMPLETED;
                break;

            case StationDownloadRunnable.DOWNLOAD_NO_NEAR_STATION:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETED;
                break;

            case StationDownloadRunnable.DONWLOAD_STATION_LIST_FAIL:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);
    }


    @Override
    public void handleListTaskState(int state) {

        int outstate = -1;
        switch (state) {

            case StationListRunnable.POPULATE_STATION_LIST_COMPLETE:
                outstate = ThreadManager.POPULATE_STATION_LIST_COMPLETED;
                break;

            case StationListRunnable.POPULATE_STATION_LIST_FAIL:
                outstate = ThreadManager.POPULATE_STATION_LIST_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }



    // Return references to OpinetStationListFragment, StationListView.
    /*
    public OpinetStationListFragment getStationListFragment() {

        if (mWeakFragment != null) {
            return mWeakFragment.get();
        }

        return null;
    }


    public StationListView getStationListView() {
        if (mWeakListView != null) return mWeakListView.get();
        else return null;
    }
    */
}