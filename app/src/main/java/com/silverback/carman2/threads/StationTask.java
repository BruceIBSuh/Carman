package com.silverback.carman2.threads;


import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.view.View;

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
        StationListRunnable.StationListMethod {
        //StationInfoRunnable.StationInfoMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationTask.class);

    // Objects
    /*
     * Creates a weak reference to the ImageView in this object. The weak
     * reference prevents memory leaks and crashes, because it automatically tracks the "state" of
     * the variable it backs. If the reference becomes invalid, the weak reference is garbage-
     * collected.
     * This technique is important for referring to objects that are part of a component lifecycle.
     * Using a hard reference may cause memory leaks as the value continues to change; even worse,
     * it can cause crashes if the underlying component is destroyed. Using a weak reference to
     * a View ensures that the reference is more transitory in nature.
     */
    private WeakReference<StationRecyclerView> mWeakRecyclerView;
    private Runnable mStationListRunnable;
    //private Runnable mStationInfoRunnable;
    private List<Opinet.GasStnParcelable> mStationList;



    private List<Opinet.GasStnParcelable> mStationInfoList;
    private Location mLocation;
    private String[] defaultParams;
    private Opinet.GasStnParcelable station;

    private static ThreadManager sThreadManager;

    // Fields
    //private int count;

    // Constructor
    StationTask(Context context) {
        super();
        //mStationInfoList = new ArrayList<>();
        mStationListRunnable = new StationListRunnable(context, this);
        //mStationInfoRunnable = new StationInfoRunnable(context, this);
    }

    void initStationTask(
            ThreadManager threadManager, StationRecyclerView view, String[] params, Location location) {

        sThreadManager = threadManager;
        defaultParams = params;
        mLocation = location;
        mWeakRecyclerView = new WeakReference<>(view);
        //mInformedStationList = new ArrayList<>();
    }

    /*
    //Long monitor contention with owner pool-1-thread-2 (15944)
    //at void com.silverback.carman2.threads.StationInfoRunnable.run()(StationInfoRunnable.java:-1)

    void initStationInfo(Opinet.GasStnParcelable station) {
        this.station = station;
    }
    */


    // Get Runnables to be called in ThreadPool.executor()
    Runnable getStationListRunnable() { return mStationListRunnable; }
    //Runnable getStationInfoRunnalbe() { return mStationInfoRunnable; }


    void recycle() {

        if(mWeakRecyclerView != null) {
            mWeakRecyclerView.clear();
            mWeakRecyclerView = null;
        }

        /*
        // Deletes the weak reference to the imageView
        if (mWeakFragment != null) {
            mWeakFragment.clear();
            mWeakFragment = null;
        }

        if (mWeakListView != null) {
            mWeakListView.clear();
            mWeakListView = null;
        }
        */

        mStationList = null;
        //defaultParams = null;

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


    // Invoked by handleMessage in ThreadManager to retrieve a station list.
    List<Opinet.GasStnParcelable> getStationList() {
        return mStationList;
    }

    /*
    @Override
    public void addStationInfo(Opinet.GasStnParcelable station) {
        mStationInfoList.add(station);
    }
    */


    // Check if all the StationInfoRunnables are complete compared with the count that is equal to
    // the size of the StationList.

    /*
    @Override
    public void addCount() {
        count++;
    }

    @Override
    public int getStationIndex() {
        return count;
    }


    @Override
    public Opinet.GasStnParcelable getStation() {
        return station;
    }


    */




    // Callback by StationInfoRunnable to get back Opinet.GasStnParcelable modified with
    // adding additional information(e.g. Car wahs here), then add it to a new List.
    /*
    @Override
    public void initStationInfo(Opinet.GasStnParcelable parcelable) {
        mInformedStationList.add(parcelable);
    }
    */

    @Override
    public void handleStationTaskState(int state) {
        int outState = -1;
        switch (state) {
            case StationListRunnable.DOWNLOAD_NEAR_STATIONS_COMPLETE:
                outState = ThreadManager.DOWNLOAD_STATION_LIST_COMPLETE;
                break;

            case StationInfoRunnable.DOWNLOAD_STATION_INFO_COMPLETE:
                log.i("Opinet.GasStationParcelable:");
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


    /*
    List<Opinet.GasStnParcelable> getStationInfoList() {
        //return mInformedStationList;
        return mStationInfoList;
    }
    */

    // Save the downloaded near station list in the designated file location.
    /*
    private Uri saveNearStationInfo(List<Opinet.GasStnParcelable> list) {

        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);

            return Uri.fromFile(file);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        return null;
    }
    */
}