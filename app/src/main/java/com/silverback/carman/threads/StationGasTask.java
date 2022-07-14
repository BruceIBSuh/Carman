package com.silverback.carman.threads;

import android.location.Location;
import android.util.SparseArray;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationViewModel;

import java.util.List;

public class StationGasTask extends ThreadTask implements
        StationGasRunnable.StationListMethod,
        StationInfoRunnable.StationInfoMethods {
        //FirestoreGetRunnable.FireStoreGetMethods,
        //FirestoreSetRunnable.FireStoreSetMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasTask.class);

    // Constants
    static final int DOWNLOAD_NEAR_STATIONS = 1;
    static final int DOWNLOAD_CURRENT_STATION = 2;
    static final int DOWNLOAD_STATION_INFO = 3;
    //static final int FIRESTORE_GET_COMPLETE = 3;
    //static final int FIRESTORE_SET_COMPLETE = 4;
    static final int DOWNLOAD_NEAR_STATIONS_FAIL = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAIL = -2;
    static final int TASK_FAILED = -3;
    //static final int FIRESTORE_GET_FAIL = -3;
    //static final int FIRESTORE_SET_FAIL = -4;

    // Objects
    private StationViewModel viewModel;
    //private WeakReference<StationViewModel> weakModelReference;
    private final Runnable mStnListRunnable;
    //private final Runnable mStnInfoRunnable;
    //private final Runnable mFireStoreSetRunnable;
    //private final Runnable mFireStoreGetRunnable;
    //private List<Opinet.GasStnParcelable> mStationList; //used by StationGasRunnable
    private List<StationGasRunnable.Item> mStationList; //used by StationGasRunnable
    //private final SparseBooleanArray sparseBooleanArray;
    private final SparseArray<StationInfoRunnable.Info> mStationInfoArray;

    //private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    //private Opinet.GasStnParcelable mCurrentStation;
    private Location mLocation;
    private String[] defaultParams;
    private int count;

    // Constructor
    StationGasTask() {
        super();
        mStnListRunnable = new StationGasRunnable(this);
        mStationInfoArray = new SparseArray<>();
        //mStnInfoRunnable = new StationInfoRunnable(this);
        //mFireStoreGetRunnable = new FirestoreGetRunnable(this);
        //mFireStoreSetRunnable = new FirestoreSetRunnable(this);
        //sparseBooleanArray = new SparseBooleanArray();

    }

    void initStationTask(StationViewModel viewModel, Location location, String[] params) {
        defaultParams = params;
        mLocation = location;
        this.viewModel = viewModel;
        log.i("Gas Location:%s", mLocation);
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStnListRunnable() { return mStnListRunnable; }
    Runnable getStnInfoRunnable(int index) {
        StationGasRunnable.Item item = mStationList.get(index);
        return new StationInfoRunnable(index, item, this);
    }
    //Runnable getFireStoreRunnable() { return mFireStoreGetRunnable; }
    //Runnable setFireStoreRunnalbe() { return mFireStoreSetRunnable; }

    // MUST BE careful to recycle variables. Otherwise, the app may break down.
    void recycle() {
        log.i("recycle task");
    }

    // The following  callbacks are invoked by StationGasRunnable to retrieve stations within
    // a radius and location, then give them back by setStationList().
    @Override
    public String[] getDefaultParam() {
        return defaultParams;
    }

    @Override
    public Location getStationLocation() {
        return mLocation;
    }

    // Callback invoked by StationGasRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setNearStationList(List<StationGasRunnable.Item> stationList) {
        mStationList = stationList;
        count = mStationList.size();
        viewModel.getNearStationList().postValue(mStationList);
    }

    @Override
    public void setCurrentStation(StationGasRunnable.Item station) {
        viewModel.getCurrentStation().postValue(station);
    }

    @Override
    public void setStationInfo(int index, StationInfoRunnable.Info info) {
        mStationInfoArray.put(index, info);
        //if(count == mStationList.size()) {
        if(count == mStationInfoArray.size()) {
            viewModel.getStationInfoArray().postValue(mStationInfoArray);
        }
        //count++;
    }

    @Override
    public void notifyException(String msg) {
        //log.i("Exception occurred: %s", msg);
        viewModel.getExceptionMessage().postValue(msg);
        //weakModelReference.get().getExceptionMessage().postValue(msg);
    }

    /*
    @Override
    public String getStationId() {
        return stnId;
    }
    */
    // FirestoreGetRunnable invokes this for having the near stations retrieved by StationGasRunnable,
    // each of which is queried for whether it has the carwash or has been visited.
    /*
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }
     */

    //@Override
    public List<StationGasRunnable.Item> getStationList() {
        return mStationList;
    }

    @Override
    public void handleTaskState(int state) {
        int outState = -1;
        switch (state) {
            case DOWNLOAD_NEAR_STATIONS:
                outState = sThreadManager.DOWNLOAD_NEAR_STATIONS;
                break;

            case DOWNLOAD_CURRENT_STATION:
                outState = sThreadManager.DOWNLOAD_CURRENT_STATION;
                break;

            case DOWNLOAD_STATION_INFO:
                outState= sThreadManager.TASK_COMPLETE;
                break;

            /*
            case FIRESTORE_GET_COMPLETE:
                outState = sThreadManager.FIRESTORE_STATION_GET_COMPLETED;
                break;

            case FIRESTORE_SET_COMPLETE:
                outState = sThreadManager.FIRESTORE_STATION_SET_COMPLETED;
                break;
            */
            case DOWNLOAD_NEAR_STATIONS_FAIL:
                //viewModel.getNearStationList().postValue(mStationList);
                //outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                //break;

            case DOWNLOAD_CURRENT_STATION_FAIL:
                //viewModel.getCurrentStation().postValue(null);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;
            /*
            case FIRESTORE_GET_FAIL:
                break;

            case FIRESTORE_SET_FAIL:
                break;

            */
            default: break;
        }

        sThreadManager.handleState(this, outState);
    }

}
