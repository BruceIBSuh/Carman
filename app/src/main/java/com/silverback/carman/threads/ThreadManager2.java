package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager2 {

    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadManager2.class);

    // Constants
    static final int FETCH_LOCATION_COMPLETED = 100;
    static final int DOWNLOAD_NEAR_STATIONS = 101;
    //static final int DOWNLOAD_CURRENT_STATIONS = 102;
    static final int FIRESTORE_STATION_GET_COMPLETED = 103;
    //static final int FIRESTORE_STATION_SET_COMPLETED = 104;

    // Determine the threadpool parameters.
    private static final int CORE_POOL_SIZE = 4;// Sets the initial threadpool size to 4
    private static final int MAXIMUM_POOL_SIZE = 4;// Sets the maximum threadpool size to 4
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;// Sets the Time Unit to seconds

    // Objects
    private static final ThreadManager2 sInstance; //Singleton instance of the class
    private final BlockingQueue<Runnable> mWorkQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final Handler mMainHandler;

    private static DistCodeDownloadTask distCodeTask;
    private static GasPriceTask gasPriceTask;
    private static LocationTask locationTask;
    private static StationListTask stnListTask;

    // Initializie a static block that sets class fields
    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS; //The time unit for "keep alive" is in seconds
        sInstance = new ThreadManager2();// Creates a single static instance of ThreadManager
    }


    // Constructor
    private ThreadManager2() {
        mWorkQueue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mWorkQueue
        );

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //ThreadTask task = (ThreadTask)msg.obj;
                //recycleTask(task);
            }
        };
    }

    // Get Singleton ThreadManager instance
    public static ThreadManager2 getInstance() {
        return sInstance;
    }

    // Handles state messages for a particular task object
    void handleState(ThreadTask task, int state) {
        Message msg = mMainHandler.obtainMessage(state, task);
        switch(state) {
            // StationListTask contains multiple Runnables of StationListRunnable, FirestoreGetRunnable,
            // and FirestoreSetRunnable to get the station data b/c the Opinet provides related data
            // in different URLs. This process will continue until Firetore will complete to hold up
            // the data in a unified form.

            // StationListRunnable downloads near stations or the current station from the Opinet,
            // the id(s) of which is used to check if the station(s) has been already saved in
            // Firestore. Otherwise, add the station(s) to Firestore w/ the carwash field left null.
            // Continuously, FireStoreSetRunnable downloads the additional data of the station(s)
            // from the Opinet and update other fields including the carwash field in Firestore.
            case DOWNLOAD_NEAR_STATIONS:
                threadPoolExecutor.execute(((StationListTask)task).getFireStoreRunnable());
                //msg.sendToTarget();
                break;

            // In case FireStore has no record as to a station,
            case FIRESTORE_STATION_GET_COMPLETED:
                // Save basic information of stations in FireStore
                threadPoolExecutor.execute(((StationListTask) task).setFireStoreRunnalbe());
                //msg.sendToTarget();
                break;

            default:
                log.i("handle task: %s", task);
                msg.sendToTarget();
                break;
        }
    }


    // Download the district code from Opinet, which is fulfilled only once when the app runs first
    // time.
    public static DistCodeDownloadTask saveDistrictCodeTask(Context context, OpinetViewModel model) {
        if(distCodeTask == null) distCodeTask = new DistCodeDownloadTask(context, model);
        sInstance.threadPoolExecutor.execute(distCodeTask.getOpinetDistCodeRunnable());
        return distCodeTask;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public static GasPriceTask startGasPriceTask(
            Context context, OpinetViewModel model, String distCode, String stnId) {

        if(gasPriceTask == null) gasPriceTask = new GasPriceTask(context);
        gasPriceTask.initPriceTask(model, distCode, stnId);

        sInstance.threadPoolExecutor.execute(gasPriceTask.getAvgPriceRunnable());
        sInstance.threadPoolExecutor.execute(gasPriceTask.getSidoPriceRunnable());
        sInstance.threadPoolExecutor.execute(gasPriceTask.getSigunPriceRunnable());
        sInstance.threadPoolExecutor.execute(gasPriceTask.getStationPriceRunnable());

        return gasPriceTask;
    }

    public static LocationTask fetchLocationTask(Context context, LocationViewModel model){
        if(locationTask == null) locationTask = new LocationTask(context);
        locationTask.initLocationTask(model);
        sInstance.threadPoolExecutor.execute(locationTask.getLocationRunnable());
        return locationTask;
    }


    // Download stations around the current location from Opinet given the current location fetched
    // by LocationTask and defaut params transferred from OpinetStationListFragment
    public static StationListTask startStationListTask(
            StationListViewModel model, Location location, String[] params) {
        if(stnListTask == null) stnListTask = new StationListTask();
        stnListTask.initStationTask(model, location, params);
        sInstance.threadPoolExecutor.execute(stnListTask.getStationListRunnable());
        return stnListTask;
    }

    @SuppressWarnings("all")
    public static synchronized void cancelAllThreads() {

        ThreadTask[] taskDownloadArray = new ThreadTask[sInstance.mWorkQueue.size()];
        //ThreadTask[] taskDecodeArray = new ThreadTask[sInstance.mDecodeWorkQueue.size()];

        // Populates the array with the task objects in the queue
        sInstance.mWorkQueue.toArray(taskDownloadArray);
        //sInstance.mDecodeWorkQueue.toArray(taskDecodeArray);

        // Stores the array length in order to iterate over the array
        int taskDownloadArrayLen = taskDownloadArray.length;
        //int taskDecodeArrayLen = taskDecodeArray.length;

        //synchronized (sInstance) {
        // Iterates over the array of tasks
        for (int taskArrayIndex = 0; taskArrayIndex < taskDownloadArrayLen; taskArrayIndex++) {
            // Gets the task's current thread
            Thread thread = taskDownloadArray[taskArrayIndex].mThreadThis;
            // if the Thread exists, post an interrupt to it
            if (null != thread) {
                thread.interrupt();
            }
        }
    }

    private void recycleTask(ThreadTask task) {
        if(task instanceof LocationTask) {
            ((LocationTask)task).recycle();


        } else if(task instanceof StationListTask) {
            // Offer() should be invoked when FirestoreSetRunnable completes.
            //mStationListTaskQueue.offer((StationListTask) task);
        }

//        } else if(task instanceof GasPriceTask) {
//            log.i("GasPriceTask done");
//            ((GasPriceTask) task).recycle();
//            mGasPriceTaskQueue.offer((GasPriceTask) task);
//
//        } else if(task instanceof FavoritePriceTask) {
//            ((FavoritePriceTask)task).recycle();
//            mFavoritePriceTaskQueue.offer((FavoritePriceTask)task);
//
//        } else if(task instanceof GeocoderReverseTask) {
//            ((GeocoderReverseTask) task).recycle();
//            mTaskWorkQueue.offer(task);
//
//        } else if(task instanceof UploadBitmapTask) {
//            log.i("Recycle UploadBitmapTask");
//            ((UploadBitmapTask)task).recycle();
//            mUploadBitmapTaskQueue.offer((UploadBitmapTask)task);
//
//        } else if(task instanceof DownloadImageTask) {
//            mTaskWorkQueue.offer(task);
//
//        } else if(task instanceof DistCodeSpinnerTask) {
//            ((DistCodeSpinnerTask)task).recycle();
//            mDistCodeSpinnerTaskQueue.offer((DistCodeSpinnerTask)task);
//        }

        // Interrupt the current thread if it is of no use.
        if(task.getCurrentThread() != null) {
            log.i("Interrupt the current thread: %s", task.getCurrentThread());
            task.getCurrentThread().interrupt();
        }

    }


}
