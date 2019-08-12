package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.StationListViewModel;

import java.util.List;

public class StationListTask extends ThreadTask implements
        StationListRunnable.StationListMethod,
        FireStoreSetRunnable.FireStoreSetMethods, FireStoreGetRunnable.FireStoreGetMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListTask.class);

    // Constants
    static final int DOWNLOAD_NEAR_STATIONS_COMPLETE = 1;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETE = 2;
    static final int FIRESTORE_GET_COMPLETE = 3;
    static final int FIRESTORE_SET_COMPLETE = 4;
    static final int DOWNLOAD_NEAR_STATIONS_FAIL = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAIL = -2;
    static final int DOWNLOAD_NO_STATION = -3;

    // Objects
    private StationListViewModel viewModel;
    private Runnable mStationListRunnable;
    private Runnable mFireStoreSetRunnable;
    private Runnable mFireStoreGetRunnable;
    private List<Opinet.GasStnParcelable> mStationList; //used by StationListRunnable

    //private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    //private Opinet.GasStnParcelable mCurrentStation;
    private Location mLocation;
    private String[] defaultParams;

    // Constructor
    StationListTask(Context context) {
        super();

        mStationListRunnable = new StationListRunnable(context, this);
        mFireStoreSetRunnable = new FireStoreSetRunnable(this);
        mFireStoreGetRunnable = new FireStoreGetRunnable(this);
    }

    void initStationTask(StationListViewModel model, Location location, String[] params) {
        defaultParams = params;
        mLocation = location;
        //mStationInfoList = new ArrayList<>();
        viewModel = model;
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    Runnable getFireStoreRunnable() { return mFireStoreGetRunnable; }
    Runnable setFireStoreRunnalbe() { return mFireStoreSetRunnable; }

    void recycle() {
        mStationList = null;
    }

    // Callback invoked by StationListRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public synchronized void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
        log.i("Download Thread: %s", thread);
    }

    // The following  callbacks are invoked by StationListRunnable to retrieve stations within
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
        viewModel.getStationListLiveData().postValue(list);
    }

    @Override
    public void setCurrentStation(Opinet.GasStnParcelable station) {
        //postValue() used in worker thread. In UI thread, use setInputValue().
        viewModel.getCurrentStationLiveData().postValue(station);
    }

    @Override
    public void setStationInfo(int position, boolean b) {
        viewModel.setStationExtraInfo(position, b);
    }

    // FireStoreGetRunnable invokes this for having the near stations retrieved by StationListRunnable,
    // each of which is queried for whether it has the carwash or has been visited.
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch (state) {
            case DOWNLOAD_NEAR_STATIONS_COMPLETE:
                log.i("DOWNLOAD_NEAR_STATIONS_COMPLETE");
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_COMPLETED;
                break;

            case DOWNLOAD_CURRENT_STATION_COMPLETE:
                outState = ThreadManager.DOWNLOAD_CURRENT_STATION_COMPLETED;
                break;

            case FIRESTORE_GET_COMPLETE:
                log.i("FireStore_Get_Complete");
                outState = ThreadManager.FIRESTORE_STATION_GET_COMPLETED;
                break;

            case FIRESTORE_SET_COMPLETE:
                log.i("FireStore Set Complete");
                outState = ThreadManager.FIRESTORE_STATION_SET_COMPLETED;
                break;

            case DOWNLOAD_CURRENT_STATION_FAIL:
                viewModel.getCurrentStationLiveData().postValue(null);
                outState = ThreadManager.DOWNLOAD_CURRENT_STATION_FAILED;
                break;

            case DOWNLOAD_NEAR_STATIONS_FAIL:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }
}