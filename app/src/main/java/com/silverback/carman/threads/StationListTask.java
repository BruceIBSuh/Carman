package com.silverback.carman.threads;

import android.location.Location;
import android.util.SparseBooleanArray;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.List;

public class StationListTask extends ThreadTask implements
        StationListRunnable.StationListMethod,
        FirestoreGetRunnable.FireStoreGetMethods,
        FirestoreSetRunnable.FireStoreSetMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListTask.class);

    // Constants
    static final int DOWNLOAD_NEAR_STATIONS = 1;
    static final int DOWNLOAD_CURRENT_STATION = 2;
    static final int FIRESTORE_GET_COMPLETE = 3;
    static final int FIRESTORE_SET_COMPLETE = 4;
    static final int DOWNLOAD_NEAR_STATIONS_FAIL = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAIL = -2;

    // Objects
    private StationListViewModel viewModel;
    private final Runnable mStationListRunnable;
    private final Runnable mFireStoreSetRunnable;
    private final Runnable mFireStoreGetRunnable;
    private List<Opinet.GasStnParcelable> mStationList; //used by StationListRunnable
    private SparseBooleanArray sparseBooleanArray;

    //private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    //private Opinet.GasStnParcelable mCurrentStation;
    private Location mLocation;
    private String[] defaultParams;
    private String stnId;

    // Constructor
    StationListTask() {
        super();
        mStationListRunnable = new StationListRunnable(this);
        mFireStoreGetRunnable = new FirestoreGetRunnable(this);
        mFireStoreSetRunnable = new FirestoreSetRunnable(this);
        sparseBooleanArray = new SparseBooleanArray();

    }

    void initStationTask(StationListViewModel viewModel, Location location, String[] params) {
        defaultParams = params;
        mLocation = location;
        this.viewModel = viewModel;
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    Runnable getFireStoreRunnable() { return mFireStoreGetRunnable; }
    Runnable setFireStoreRunnalbe() { return mFireStoreSetRunnable; }

    // MUST BE careful to recycle variables. Otherwise, the app may break down.
    void recycle() {
        //mStationList = null;
    }

    // Callback invoked by StationListRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public synchronized void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setStationList(List<Opinet.GasStnParcelable> list) {
        mStationList = list;
        viewModel.getNearStationList().postValue(mStationList);
    }

    @Override
    public void setStationId(String stnId) {
        this.stnId = stnId;
    }

    @Override
    public void setCarWashInfo(int position, boolean isWash) {
        log.i("SparseArray: %s, %s", sparseBooleanArray, mStationList);
        sparseBooleanArray.put(position, isWash);
        // Check if the SparseBooleanArray size always equals to StationList size. Otherwise, it will
        // incur a unexpectable result.
        if(sparseBooleanArray.size() == mStationList.size()) {
            log.i("Invoke CarWash viewmodel");
            viewModel.getStationCarWashInfo().postValue(sparseBooleanArray);
        }
    }


    @Override
    public void setCurrentStation(Opinet.GasStnParcelable station) {
        //postValue() used in worker thread. In UI thread, use setInputValue().
        viewModel.getCurrentStation().postValue(station);
    }

    /*
    @Override
    public void notifyException(String msg) {
        //log.i("Exception occurred: %s", msg);
        //viewModel.getExceptionMessage().postValue(msg);
    }
    */


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
    public String getStationId() {
        return stnId;
    }
    // FirestoreGetRunnable invokes this for having the near stations retrieved by StationListRunnable,
    // each of which is queried for whether it has the carwash or has been visited.
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    @Override
    public void handleTaskState(int state) {
        int outState = -1;
        switch (state) {
            case DOWNLOAD_NEAR_STATIONS:
                outState = ThreadManager2.DOWNLOAD_NEAR_STATIONS;
                break;

            case DOWNLOAD_CURRENT_STATION:
                outState = ThreadManager2.DOWNLOAD_CURRENT_STATION;
                break;

            case FIRESTORE_GET_COMPLETE:
                outState = ThreadManager2.FIRESTORE_STATION_GET_COMPLETED;
                break;

            case FIRESTORE_SET_COMPLETE:
                outState = ThreadManager2.FIRESTORE_STATION_SET_COMPLETED;
                break;

            case DOWNLOAD_NEAR_STATIONS_FAIL:
                viewModel.getNearStationList().postValue(mStationList);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;

            case DOWNLOAD_CURRENT_STATION_FAIL:
                viewModel.getCurrentStation().postValue(null);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;

            default: break;
        }

        sThreadManager.handleState(this, outState);
    }

}