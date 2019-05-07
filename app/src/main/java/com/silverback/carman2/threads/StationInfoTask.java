package com.silverback.carman2.threads;

import android.content.Context;
import android.view.View;

import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.views.StationRecyclerView;

import java.lang.ref.WeakReference;

public class StationInfoTask extends ThreadTask implements
        StationInfoRunnable.StationInfoMethods,
        FireStoreUpdateRunnable.FireStoreUpdateMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoTask.class);

    // Objects
    private static ThreadManager sThreadManager;
    private Runnable mStationInfoRunnable, mFireStoreUpdateRunnable;
    //private WeakReference<StationMapActivity> mWeakActivity;
    private Opinet.GasStationInfo stationInfo;
    private String stnName, stnId;

    // Constructor
    StationInfoTask(Context context) {

        /*
        try {
            mWeakActivity = new WeakReference<>((StationMapActivity)context);
        } catch(ClassCastException e) {
            log.e("ClassCastException: %s", e.getMessage());
        }
        */

        mStationInfoRunnable = new StationInfoRunnable(context, this);
        mFireStoreUpdateRunnable = new FireStoreUpdateRunnable(this);
    }

    void initStationTask(ThreadManager threadManager, String stationName, String stationId) {
        sThreadManager = threadManager;
        stnId = stationId;
        stnName = stationName;
        log.i("StationID: %s", stnId);
    }

    // Getter for Runnables
    Runnable getStationMapInfoRunnable() {
        return mStationInfoRunnable;
    }
    Runnable getFireStoreUpdateRunnable() { return mFireStoreUpdateRunnable; }

    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    @Override
    public String getStationName() {
        return stnName;
    }

    @Override
    public void setStationInfo(Opinet.GasStationInfo info) {
        stationInfo = info;
    }
    public Opinet.GasStationInfo getStationInfo() {
        return stationInfo;
    }

    @Override
    public String getStnID() {
        return stnId;
    }

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch(state) {
            case StationInfoRunnable.DOWNLOAD_STATION_INFO_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STATION_INFO_COMPLETED;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_FAIL:
                outState = ThreadManager.DOWNLOAD_STN_MAPINFO_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);
    }
}