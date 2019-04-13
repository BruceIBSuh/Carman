package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.lang.ref.WeakReference;

public class StationInfoTask extends ThreadTask implements
        StationInfoRunnable.StationInfoMethods,
        FireStoreUpdateRunnable.FireStoreUpdateMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoTask.class);

    // Objects
    private static ThreadManager sThreadManager;
    private Runnable mStationInfoRunnable, mFireStoreUpdateRunnable;
    private WeakReference<Context> mWeakContext;
    private Opinet.GasStationInfo stationInfo;
    private String stnName, stnId;

    // Constructor
    StationInfoTask(Context context) {
        mWeakContext = new WeakReference<>(context);
        mStationInfoRunnable = new StationInfoRunnable(mWeakContext, this);
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



    void recycle() {
        if(mWeakContext != null) {
            mWeakContext.clear();
            mWeakContext = null;
        }
    }


}
