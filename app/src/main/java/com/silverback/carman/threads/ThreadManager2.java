package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager2 {

    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadManager2.class);

    // Constants
    public final int TASK_COMPLETE = 1;
    public final int TASK_FAIL = -1;

    public final int DOWNLOAD_NEAR_STATIONS = 100;
    public final int DOWNLOAD_CURRENT_STATION = 101;
    public final int FIRESTORE_STATION_GET_COMPLETED = 103;
    public final int FIRESTORE_STATION_SET_COMPLETED = 104;

    static final int FETCH_LOCATION_COMPLETED = 102;
    //static final int DOWNLOAD_NEAR_STATIONS = 101;
    //static final int DOWNLOAD_CURRENT_STATION = 102;
    //static final int FIRESTORE_STATION_GET_COMPLETED = 103;
    //static final int FIRESTORE_STATION_SET_COMPLETED = 104;
    static final int DISTCODE_COMPLETED = 105;
    static final int UPLOAD_BITMAP_COMPLETED = 106;
    static final int UPLOAD_BITMAP_FAILED = -106;

    static final int FETCH_LOCATION_FAILED = -100;
    static final int DOWNLOAD_STATION_FAILED = -101;
    static final int DISTCODE_FAILED = -102;

    // Determine the threadpool parameters.
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;// Sets the Time Unit to seconds

    // Objects
    //private static final InnerInstanceClazz sInstance; //Singleton instance of the class
    private final BlockingQueue<Runnable> mWorkerThreadQueue;
    private final BlockingQueue<StationGasTask> mStnListTaskQueue;
    private final BlockingQueue<StationEvTask> mElecListTaskQueue;
    private final BlockingQueue<StationHydroTask> mHydroListTaskQueue;
    private final BlockingQueue<LocationTask> mLocationTaskQueue;
    private final BlockingQueue<GeocoderReverseTask> mGeocoderReverseTaskQueue;
    private final BlockingQueue<GasPriceTask> mGasPriceTaskQueue;
    private final BlockingQueue<FavoritePriceTask> mFavoritePriceTaskQueue;
    private final BlockingQueue<DistCodeSpinnerTask> mDistCodeSpinnerTaskQueue;
    private final BlockingQueue<UploadBitmapTask> mUploadBitmapTaskQueue;
    

    private final BlockingQueue<UpdatePostTask> mUploadPostTaskQueue2;

    private final ThreadPoolExecutor threadPoolExecutor;
    private final Handler mMainHandler;

    private ThreadTask threadTask;
    private DistCodeDownloadTask distCodeTask;
    private GeocoderTask geocoderTask;
    private GeocoderReverseTask geocoderReverseTask;
    private DistCodeSpinnerTask distSpinnerTak;
    private GasPriceTask gasPriceTask;
    private LocationTask locationTask;
    private StationGasTask stnListTask;
    private UploadBitmapTask uploadBitmapTask;

    // Constructor private
    private ThreadManager2() {
        //super();
        mWorkerThreadQueue = new LinkedBlockingQueue<>();

        mStnListTaskQueue = new LinkedBlockingQueue<>();
        mElecListTaskQueue = new LinkedBlockingQueue<>();
        mHydroListTaskQueue = new LinkedBlockingQueue<>();
        mLocationTaskQueue = new LinkedBlockingQueue<>();
        mGeocoderReverseTaskQueue = new LinkedBlockingQueue<>();
        mGasPriceTaskQueue = new LinkedBlockingQueue<>();
        mFavoritePriceTaskQueue = new LinkedBlockingQueue<>();
        mUploadBitmapTaskQueue = new LinkedBlockingQueue<>();
        mDistCodeSpinnerTaskQueue = new LinkedBlockingQueue<>();

        mUploadPostTaskQueue2 = new LinkedBlockingQueue<>();

        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mWorkerThreadQueue
        );

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                ThreadTask task = (ThreadTask) msg.obj;
                if(task instanceof UploadBitmapTask) {
                    log.i("upload compressed bitmap done");
                    recycleTask(task);
                } else if(task instanceof StationEvTask) {
                    log.i("ev station task");
                    recycleTask(task);
                }

            }
        };
    }

    // Singleton Initialization: LazyHolder type.
    private static class InnerClazz {
        private static final ThreadManager2 sInstance = new ThreadManager2();
    }

    public static ThreadManager2 getInstance() {
        return InnerClazz.sInstance;
    }

    // Handles state messages for a particular task object
    void handleState(ThreadTask task, int state) {
        Message msg = mMainHandler.obtainMessage(state, task);
        switch(state) {
            // StationGasTask contains multiple Runnables of StationGasRunnable, FirestoreGetRunnable,
            // and FirestoreSetRunnable to get the station data b/c the Opinet provides related data
            // in different URLs. This process will continue until Firetore will complete to hold up
            // the data in a unified form.
            //
            // StationGasRunnable downloads near stations or the current station from the Opinet,
            // the id(s) of which is used to check if the station(s) has been already saved in
            // Firestore. Otherwise, add the station(s) to Firestore w/ the carwash field left null.
            // Continuously, FireStoreSetRunnable downloads the additional data of the station(s)
            // from the Opinet and update other fields including the carwash field in Firestore.
            case DOWNLOAD_CURRENT_STATION:
                break;
            case DOWNLOAD_NEAR_STATIONS:
                InnerClazz.sInstance.threadPoolExecutor.execute(
                        ((StationGasTask)task).getFireStoreRunnable());
                break;
            // In case FireStore has no record as to a station,
            case FIRESTORE_STATION_GET_COMPLETED:
                // Save basic information of stations in FireStore
                log.i("upload station data: %s", task);
                InnerClazz.sInstance.threadPoolExecutor.execute(
                        ((StationGasTask)task).setFireStoreRunnalbe());
                break;

            case FIRESTORE_STATION_SET_COMPLETED:
                log.i("upload station info to Firestore");
                msg.sendToTarget();
                break;

            case TASK_COMPLETE:
                if(task instanceof StationEvTask) {
                    Message evMessage = mMainHandler.obtainMessage(state, task);
                    evMessage.sendToTarget();
                }
            case TASK_FAIL:
                if(task instanceof StationEvTask) msg.sendToTarget();
                break;

            default: msg.sendToTarget();
        }
    }


    @SuppressWarnings("all")
    public synchronized void cancelAllThreads() {
        ThreadTask[] taskDownloadArray = new ThreadTask[InnerClazz.sInstance.mWorkerThreadQueue.size()];
        //ThreadTask[] taskDecodeArray = new ThreadTask[sInstance.mDecodeWorkQueue.size()];

        // Populates the array with the task objects in the queue
        InnerClazz.sInstance.mWorkerThreadQueue.toArray(taskDownloadArray);
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



    // Download the district code from Opinet, which is fulfilled only once when the app runs first
    // time.
    public DistCodeDownloadTask saveDistrictCodeTask(Context context, OpinetViewModel model) {
        //DistCodeDownloadTask distCodeTask = (DistCodeDownloadTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
        if(distCodeTask == null) distCodeTask = new DistCodeDownloadTask(context, model);
        InnerClazz.sInstance.threadPoolExecutor.execute(distCodeTask.getOpinetDistCodeRunnable());
        return distCodeTask;
    }

    public GeocoderTask startGeocoderTask(Context context, LocationViewModel model, String addrs) {
        //GeocoderTask geocoderTask = (GeocoderTask)sInstance.mTaskWorkQueue.poll();
        //if(geocoderTask == null)
            geocoderTask = new GeocoderTask(context);
        geocoderTask.initGeocoderTask(model, addrs);
        InnerClazz.sInstance.threadPoolExecutor.execute(geocoderTask.getGeocoderRunnable());
        return geocoderTask;
    }

    public static GeocoderReverseTask startReverseGeocoderTask (
            Context context, LocationViewModel model, Location location) {

        GeocoderReverseTask geocoderReverseTask = InnerClazz.sInstance.mGeocoderReverseTaskQueue.poll();
        if(geocoderReverseTask == null) geocoderReverseTask = new GeocoderReverseTask(context);
        geocoderReverseTask.initGeocoderReverseTask(model, location);
        InnerClazz.sInstance.threadPoolExecutor.execute(geocoderReverseTask.getGeocoderRunnable());
        return geocoderReverseTask;
    }

    public static DistCodeSpinnerTask loadDistrictSpinnerTask(Context context, OpinetViewModel model, int code) {
        DistCodeSpinnerTask districtSpinnerTask = InnerClazz.sInstance.mDistCodeSpinnerTaskQueue.poll();
        if(districtSpinnerTask == null) districtSpinnerTask = new DistCodeSpinnerTask(context);
        districtSpinnerTask.initSpinnerDistCodeTask(model, code);
        InnerClazz.sInstance.threadPoolExecutor.execute(districtSpinnerTask.getDistCodeSpinnerRunnable());

        return districtSpinnerTask;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public static GasPriceTask startGasPriceTask(Context context, OpinetViewModel model, String distCode) {
        GasPriceTask gasPriceTask = InnerClazz.sInstance.mGasPriceTaskQueue.poll();
        if(gasPriceTask == null) gasPriceTask = new GasPriceTask(context);
        gasPriceTask.initPriceTask(model, distCode);

        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getAvgPriceRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getSidoPriceRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getSigunPriceRunnable());
        //InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getStationPriceRunnable());

        return gasPriceTask;
    }

    public static FavoritePriceTask startFavoritePriceTask(
            Context context, @Nullable OpinetViewModel model, String stnId, boolean isFirst) {

        FavoritePriceTask favPriceTask = InnerClazz.sInstance.mFavoritePriceTaskQueue.poll();
        if(favPriceTask == null) favPriceTask = new FavoritePriceTask(context);
        favPriceTask.initTask(model, stnId, isFirst);

        InnerClazz.sInstance.threadPoolExecutor.execute(favPriceTask.getPriceRunnableStation());
        return favPriceTask;
    }


    public static LocationTask fetchLocationTask(Context context, LocationViewModel model){
        LocationTask locationTask = InnerClazz.sInstance.mLocationTaskQueue.poll();
        if(locationTask == null) locationTask = new LocationTask(context);
        locationTask.initLocationTask(model);

        log.i("LocationTasK %s", locationTask);
        InnerClazz.sInstance.threadPoolExecutor.execute(locationTask.getLocationRunnable());
        return locationTask;
    }


    // Download stations around the current location from Opinet given the current location fetched
    // by LocationTask and defaut params transferred from OpinetStationListFragment
    public static StationGasTask startGasStationListTask(
            StationListViewModel model, Location location, String[] params) {

        StationGasTask stationGasTask = InnerClazz.sInstance.mStnListTaskQueue.poll();
        if(stationGasTask == null) stationGasTask = new StationGasTask();
        stationGasTask.initStationTask(model, location, params);
        log.i("StationGasTask: %s", stationGasTask);

        InnerClazz.sInstance.threadPoolExecutor.execute(stationGasTask.getStationListRunnable());
        return stationGasTask;
    }

    // Electric Charge Station
    public static StationEvTask startEVStatoinListTask(
            Context context, StationListViewModel model, Location location) {
        StationEvTask stationEvTask = InnerClazz.sInstance.mElecListTaskQueue.poll();
        if(stationEvTask == null) stationEvTask = new StationEvTask(context, model, location);

        for(int page = 1; page <= 5; page++) {
            Runnable elecRunnable = stationEvTask.getElecStationListRunnable(page);
            InnerClazz.sInstance.threadPoolExecutor.execute(elecRunnable);
        }

        //InnerClazz.sInstance.threadPoolExecutor.execute(stationEvTask.getElecStationListRunnable());
        return stationEvTask;
    }

    public static StationHydroTask startHydroStationListTask(
            Context context, StationListViewModel model, Location location) {

        StationHydroTask hydroStationsTask = InnerClazz.sInstance.mHydroListTaskQueue.poll();
        if(hydroStationsTask == null) hydroStationsTask = new StationHydroTask(context, model, location);
        InnerClazz.sInstance.threadPoolExecutor.execute(hydroStationsTask.getHydroListRunnable());
        return hydroStationsTask;


    }

    public static UploadBitmapTask uploadBitmapTask(
            Context context, final Uri uriImage, final int position, ImageViewModel model) {

        UploadBitmapTask uploadBitmapTask = InnerClazz.sInstance.mUploadBitmapTaskQueue.poll();
        if(uploadBitmapTask == null) uploadBitmapTask = new UploadBitmapTask(context);

        uploadBitmapTask.initBitmapTask(uriImage, position, model);
        InnerClazz.sInstance.threadPoolExecutor.execute(uploadBitmapTask.getBitmapUploadRunnable());

        return uploadBitmapTask;
    }



    public static UpdatePostTask updatePostTask(
            Context context, Map<String, Object> post, List<String> removedImages, List<Uri> newImages) {
        UpdatePostTask updatePostTask = InnerClazz.sInstance.mUploadPostTaskQueue2.poll();

        if(updatePostTask == null) updatePostTask = new UpdatePostTask(context);
        updatePostTask.initTask(post, removedImages, newImages);
        InnerClazz.sInstance.threadPoolExecutor.execute(updatePostTask.getUploadPostRunnable2());

        return updatePostTask;
    }



    private void recycleTask(ThreadTask task) {
        if(task instanceof GasPriceTask) {
            task.recycle();
            mGasPriceTaskQueue.offer((GasPriceTask)task);
        } else if(task instanceof LocationTask) {
            task.recycle();
            mLocationTaskQueue.offer((LocationTask)task);
        } else if(task instanceof StationGasTask) {
            task.recycle();
            mStnListTaskQueue.offer((StationGasTask)task);
        } else if(task instanceof UploadBitmapTask) {
            task.recycle();
            mUploadBitmapTaskQueue.offer((UploadBitmapTask)task);
        } else if(task instanceof StationEvTask) {
            task.recycle();
            mElecListTaskQueue.offer((StationEvTask)task);
        }
    }


}