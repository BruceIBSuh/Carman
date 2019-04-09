package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.views.StationRecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class StationTask extends ThreadTask implements
        StationListRunnable.StationListMethod,
        StationInfoRunnable.StationInfoMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationTask.class);

    // Objects
    private Context context;
    private WeakReference<StationRecyclerView> mWeakRecyclerView;
    private Runnable mStationListRunnable;
    private Runnable mStationInfoRunnable;
    private List<Opinet.GasStnParcelable> mStationList; //used by StationListRunnable

    private List<Opinet.GasStnParcelable> mStationInfoList; //used by StationInfoRunnable
    private Location mLocation;
    private String[] defaultParams;
    private int count = 0;

    private static ThreadManager sThreadManager;

    // Constructor
    StationTask(Context context) {
        super();
        this.context = context;
        mStationListRunnable = new StationListRunnable(context, this);
        mStationInfoRunnable = new StationInfoRunnable(this);
    }

    void initStationTask(
            ThreadManager threadManager, StationRecyclerView view, String[] params, Location location) {

        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
        mWeakRecyclerView = new WeakReference<>(view);
        mStationInfoList = new ArrayList<>();
    }

    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    Runnable getStationInfoRunnalbe() { return mStationInfoRunnable; }

    void recycle() {
        if(mWeakRecyclerView != null) {
            mWeakRecyclerView.clear();
            mWeakRecyclerView = null;
        }
        mStationList = null;
    }

    // Callback invoked by StationListRunnable and StationInfoRunnable as well to set the current
    // thread of each Runnables.
    @Override
    public void setStationTaskThread(Thread thread) {
        setCurrentThread(thread);
        log.i("Download Thread: %s", thread);
    }

    // The following 3 callbacks are invoked by StationListRunnable to retrieve stations within
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
    }

    /*
     * The followng 3 methods override the methods defined in StationInfoRunnable.StationInfoMethods.
     * getStationList(): pass the station list retrieved by StationListRunnable to StationInfoRunnable.
     * getStationIndex():
     * addStationInfo()
     */
    @Override
    public List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    // Indicate an index of the station list fetched by StationListRunnable.
    @Override
    public int getStationIndex() {
        return count ++;
    }


    @Override
    public void addStationInfo(Opinet.GasStnParcelable station) {
        mStationInfoList.add(station);
    }



    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch (state) {
            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STATION_LIST_COMPLETE;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_COMPLETE:
                log.i("Opinet.GasStationParcelable:");
                if(saveNearStationInfo(mStationInfoList))
                    outState= ThreadManager.DOWNLOAD_STATION_INFO_COMPLETE;
                break;

            case StationListRunnable.DOWNLOAD_CURRENT_STATION_FAILED:
                outState = ThreadManager.DOWNLOAD_NO_STATION_COMPLETE;
                break;

            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_FAILED:
                outState = ThreadManager.DOWNLOAD_NEAR_STATIONS_FAILED;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_FAILED:
                break;

            default:
                break;
        }

        sThreadManager.handleState(this, outState);
    }

    // Save the station list fetched by StationListRunnable and added with car wash info by
    // StationInfoRunnable.
    @SuppressWarnings("UnusedReturnValue")
    private boolean saveNearStationInfo(List<Opinet.GasStnParcelable> list) {

        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);

        // Delete the file before saving a new list.
        if(file.exists()) {
            boolean delete = file.delete();
            if(delete) log.i("cache cleared");
        }

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);
            return true;

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        return false;
    }

}