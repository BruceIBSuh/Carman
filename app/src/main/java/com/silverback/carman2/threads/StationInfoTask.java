package com.silverback.carman2.threads;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.StationListViewModel;

public class StationInfoTask extends ThreadTask implements
        //LifecycleObserver,
        StationInfoRunnable.StationInfoMethods,
        FireStoreUpdateRunnable.FireStoreUpdateMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoTask.class);

    // Objects
    private StationListViewModel stnViewModel;
    private Runnable mStationInfoRunnable, mFireStoreUpdateRunnable;
    private Opinet.GasStationInfo stationInfo;
    private String stnName, stnId;

    // Constructor
    StationInfoTask() {
        mStationInfoRunnable = new StationInfoRunnable(this);
        mFireStoreUpdateRunnable = new FireStoreUpdateRunnable(this);
    }

    void initStationTask(StationListViewModel model, String stationName, String stationId) {
        stnViewModel = model;
        stnId = stationId;
        stnName = stationName;
    }

    // Getter for Runnables
    Runnable getStationMapInfoRunnable() { return mStationInfoRunnable;}
    Runnable updateFireStoreRunnable() { return mFireStoreUpdateRunnable; }

    void recycle(){
        log.i("recycler");
    }

    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    @Override
    public void setStationInfo(Opinet.GasStationInfo info) {
        stationInfo = info;
        stnViewModel.getStationInfoLiveData().postValue(info);
    }

    // Referenced by FireStoreUpdateRunnable
    @Override
    public Opinet.GasStationInfo getStationInfo() {
        return stationInfo;
    }


    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch(state) {
            case StationInfoRunnable.DOWNLOAD_STATION_INFO_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STATION_INFO_COMPLETED;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_FAIL:
                outState = ThreadManager.DOWNLOAD_STATION_INFO_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);
    }

}
