package com.silverback.carman.threads;

import android.location.Location;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationViewModel;

import java.util.List;

public class StationGasTask extends ThreadTask implements
        StationGasRunnable.StationListMethod, StationInfoRunnable.StationInfoMethods,
        FirestoreGetRunnable.FireStoreGetMethods,
        FirestoreSetRunnable.FireStoreSetMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasTask.class);

    // Constants
    static final int DOWNLOAD_NEAR_STATIONS = 1;
    static final int DOWNLOAD_CURRENT_STATION = 2;
    static final int FIRESTORE_GET_COMPLETE = 3;
    static final int FIRESTORE_SET_COMPLETE = 4;
    static final int DOWNLOAD_NEAR_STATIONS_FAIL = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAIL = -2;
    static final int FIRESTORE_GET_FAIL = -3;
    static final int FIRESTORE_SET_FAIL = -4;

    // Objects
    private StationViewModel viewModel;
    //private WeakReference<StationViewModel> weakModelReference;
    private final Runnable mStnListRunnable;
    private final Runnable mStnInfoRunnable;
    private final Runnable mFireStoreSetRunnable;
    private final Runnable mFireStoreGetRunnable;
    //private List<Opinet.GasStnParcelable> mStationList; //used by StationGasRunnable
    private List<StationGasRunnable.Item> mStationList; //used by StationGasRunnable
    private final SparseBooleanArray sparseBooleanArray;

    //private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    //private Opinet.GasStnParcelable mCurrentStation;
    private Location mLocation;
    private String[] defaultParams;
    private String stnId;

    // Constructor
    StationGasTask() {
        super();
        mStnListRunnable = new StationGasRunnable(this);
        mStnInfoRunnable = new StationInfoRunnable(this);
        mFireStoreGetRunnable = new FirestoreGetRunnable(this);
        mFireStoreSetRunnable = new FirestoreSetRunnable(this);
        sparseBooleanArray = new SparseBooleanArray();

    }

    void initStationTask(StationViewModel viewModel, Location location, String[] params) {
        defaultParams = params;
        mLocation = location;
        this.viewModel = viewModel;
        //weakModelReference = new WeakReference<>(viewModel);

        log.i("Location in GasTask: %s", mLocation);
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStnListRunnable() { return mStnListRunnable; }
    Runnable getStnInfoRunnable() { return mStnInfoRunnable; }
    Runnable getFireStoreRunnable() { return mFireStoreGetRunnable; }
    Runnable setFireStoreRunnalbe() { return mFireStoreSetRunnable; }

    // MUST BE careful to recycle variables. Otherwise, the app may break down.
    void recycle() {
        mStationList.clear();
    }

    // Callback invoked by StationGasRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }




    @Override
    public void setStationId(String stnId) {
        this.stnId = stnId;
    }

    @Override
    public void setCarWashInfo(int position, boolean isCarwash) {}

    /*
   @Override
   public void setNearStationList(List<Opinet.GasStnParcelable> list) {
       log.i("viewmodel test: %s", list.size());
       mStationList = list;
       viewModel.getNearStationList().postValue(mStationList);
       //weakModelReference.get().getNearStationList().postValue(mStationList);
   }
   */
    @Override
    public void setNearStationList(List<StationGasRunnable.Item> stationList) {
        mStationList = stationList;
        viewModel.getNearStationList().postValue(mStationList);
    }

    @Override
    public List<StationGasRunnable.Item> getNearStationList() {
        return mStationList;
    }

    @Override
    public void setStationInfoArray(SparseArray<StationInfoRunnable.Info> sparseArray) {
        viewModel.getStationInfo().postValue(sparseArray);
    }

    @Override
    public void setStationInfoList(List<StationGasRunnable.Item> stationList) {
        mStationList = stationList;
        //viewModel.getNearStationList().postValue(stationList);
    }

    /*
    @Override
    public void setCurrentStation(Opinet.GasStnParcelable station) {
        //postValue() used in worker thread. In UI thread, use setInputValue().
        viewModel.getCurrentStation().postValue(station);
    }

     */
    @Override
    public void setCurrentStation(StationGasRunnable.Item station) {
        viewModel.getCurrentStation().postValue(station);
    }


    @Override
    public void notifyException(String msg) {
        //log.i("Exception occurred: %s", msg);
        viewModel.getExceptionMessage().postValue(msg);
        //weakModelReference.get().getExceptionMessage().postValue(msg);
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

    @Override
    public String getStationId() {
        return stnId;
    }
    // FirestoreGetRunnable invokes this for having the near stations retrieved by StationGasRunnable,
    // each of which is queried for whether it has the carwash or has been visited.
    /*
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }
     */

    @Override
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

            case FIRESTORE_GET_COMPLETE:
                outState = sThreadManager.FIRESTORE_STATION_GET_COMPLETED;
                break;

            case FIRESTORE_SET_COMPLETE:
                outState = sThreadManager.FIRESTORE_STATION_SET_COMPLETED;
                break;

            case DOWNLOAD_NEAR_STATIONS_FAIL:
                //viewModel.getNearStationList().postValue(mStationList);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;

            case DOWNLOAD_CURRENT_STATION_FAIL:
                //viewModel.getCurrentStation().postValue(null);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;

            case FIRESTORE_GET_FAIL:
                break;

            case FIRESTORE_SET_FAIL:
                break;
            default: break;
        }

        sThreadManager.handleState(this, outState);
    }

}
