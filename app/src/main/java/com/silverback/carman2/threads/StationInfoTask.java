package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.lang.ref.WeakReference;

public class StationInfoTask extends ThreadTask implements StationInfoRunnable.StationInfoMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoTask.class);

    // Objects
    private static ThreadManager sThreadManager;
    private Runnable mStationInfoRunnable;
    private WeakReference<Context> mWeakContext;
    private Opinet.GasStationInfo stationInfo;
    private String stnId;

    // Constructor
    StationInfoTask(Context context) {
        mWeakContext = new WeakReference<>(context);
        mStationInfoRunnable = new StationInfoRunnable(mWeakContext, this);
    }

    void initStationTask(ThreadManager threadManager, String stationId) {
        sThreadManager = threadManager;
        stnId = stationId;
    }

    Runnable getStationMapInfoRunnable() {
        return mStationInfoRunnable;
    }

    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setStationInfo(Opinet.GasStationInfo info) {
        stationInfo = info;
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

    @Override
    public String getStnID() {
        return stnId;
    }

    void recycle() {
        if(mWeakContext != null) {
            mWeakContext.clear();
            mWeakContext = null;
        }
    }

    Opinet.GasStationInfo getStationInfo() {
        return stationInfo;
    }
}
