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

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasTask.class);

    // Constants
    static final int DOWNLOAD_NEAR_STATIONS = 1;
    static final int DOWNLOAD_CURRENT_STATION = 2;
    static final int DOWNLOAD_STATION_INFO = 3;
    static final int DOWNLOAD_NEAR_STATIONS_FAIL = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAIL = -2;
    static final int TASK_FAILED = -3;
    // Objects
    private StationViewModel viewModel;
    private final Runnable mStnListRunnable;
    private List<StationGasRunnable.Item> mStationList;
    private final SparseArray<StationInfoRunnable.Info> mStationInfoArray;
    private Location mLocation;
    private String[] defaultParams;
    private int count;

    // Constructor
    StationGasTask() {
        super();
        mStnListRunnable = new StationGasRunnable(this);
        mStationInfoArray = new SparseArray<>();
    }

    void initStationTask(StationViewModel viewModel, Location location, String[] params) {
        defaultParams = params;
        mLocation = location;
        this.viewModel = viewModel;
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStnListRunnable() { return mStnListRunnable; }
    Runnable getStnInfoRunnable(int index) {
        StationGasRunnable.Item item = mStationList.get(index);
        return new StationInfoRunnable(index, item, this);
    }


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
        if(count == mStationInfoArray.size()) {
            viewModel.getStationInfoArray().postValue(mStationInfoArray);
        }
    }

    @Override
    public void notifyException(String msg) {
        //log.i("Exception occurred: %s", msg);
        viewModel.getGasExceptionMessage().postValue(msg);
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

            case DOWNLOAD_NEAR_STATIONS_FAIL:
                //viewModel.getNearStationList().postValue(mStationList);
                //outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                //break;

            case DOWNLOAD_CURRENT_STATION_FAIL:
                //viewModel.getCurrentStation().postValue(null);
                outState = ThreadManager2.DOWNLOAD_STATION_FAILED;
                break;

            default: break;
        }

        sThreadManager.handleState(this, outState);
    }

}
