package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.lang.ref.WeakReference;

public class StationMapInfoTask extends ThreadTask implements StationMapInfoRunnable.MapInfoMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapInfoTask.class);

    // Objects
    private static ThreadManager sThreadManager;
    private Runnable mStationMapInfoRunnable;
    private WeakReference<Context> mWeakContext;
    private Opinet.GasStationInfo mapInfo;
    private String stnId;

    // Constructor
    StationMapInfoTask(Context context) {
        mWeakContext = new WeakReference<>(context);
        mStationMapInfoRunnable = new StationMapInfoRunnable(mWeakContext, this);
    }

    void initStationTask(ThreadManager threadManager, String stationId) {
        sThreadManager = threadManager;
        stnId = stationId;
    }

    Runnable getStationMapInfoRunnable() {
        return mStationMapInfoRunnable;
    }

    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setStationMapInfo(Opinet.GasStationInfo info) {
        mapInfo = info;
    }

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch(state) {
            case StationMapInfoRunnable.DOWNLOAD_STN_MAPINFO_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STN_MAPINFO_COMPLETED;
                break;

            case StationMapInfoRunnable.DOWNLOAD_STN_MAPINFO_FAIL:
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

    Opinet.GasStationInfo getOpinetMapInfo() {
        return mapInfo;
    }
}
