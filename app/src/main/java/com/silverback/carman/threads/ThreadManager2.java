package com.silverback.carman.threads;

import static com.google.firebase.storage.StorageTaskScheduler.sInstance;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.PagerAdapterViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
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
    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    static final int FETCH_LOCATION_COMPLETED = 100;
    static final int DOWNLOAD_NEAR_STATIONS = 101;
    static final int DOWNLOAD_CURRENT_STATION = 102;
    static final int FIRESTORE_STATION_GET_COMPLETED = 103;
    static final int FIRESTORE_STATION_SET_COMPLETED = 104;
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
    private final Queue<ThreadTask> mThreadTaskQueue;
    private final BlockingQueue<StationListTask> mStnListTaskQueue;
    private final BlockingQueue<LocationTask> mLocationTaskQueue;
    private final BlockingQueue<GasPriceTask> mGasPriceTaskQueue;
    private final BlockingQueue<UploadBitmapTask> mUploadBitmapTaskQueue;
    private final BlockingQueue<UploadPostTask> mUploadPostTaskQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final Handler mMainHandler;

    private ThreadTask threadTask;
    private DistCodeDownloadTask distCodeTask;
    private GeocoderTask geocoderTask;
    private GeocoderReverseTask geocoderReverseTask;
    private DistCodeSpinnerTask distSpinnerTak;
    private GasPriceTask gasPriceTask;
    private LocationTask locationTask;
    private StationListTask stnListTask;
    private UploadBitmapTask uploadBitmapTask;

    // Constructor private
    private ThreadManager2() {
        //super();
        mThreadTaskQueue = new LinkedBlockingQueue<>();
        mWorkerThreadQueue = new LinkedBlockingQueue<>();

        mStnListTaskQueue = new LinkedBlockingQueue<>();
        mLocationTaskQueue = new LinkedBlockingQueue<>();
        mGasPriceTaskQueue = new LinkedBlockingQueue<>();
        mUploadBitmapTaskQueue = new LinkedBlockingQueue<>();
        mUploadPostTaskQueue = new LinkedBlockingQueue<>();

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
                } else if(task instanceof UploadPostTask) {
                    log.i("upload post done");
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
            case FETCH_LOCATION_COMPLETED:
                msg.sendToTarget();
                break;
            // StationListTask contains multiple Runnables of StationListRunnable, FirestoreGetRunnable,
            // and FirestoreSetRunnable to get the station data b/c the Opinet provides related data
            // in different URLs. This process will continue until Firetore will complete to hold up
            // the data in a unified form.
            //
            // StationListRunnable downloads near stations or the current station from the Opinet,
            // the id(s) of which is used to check if the station(s) has been already saved in
            // Firestore. Otherwise, add the station(s) to Firestore w/ the carwash field left null.
            // Continuously, FireStoreSetRunnable downloads the additional data of the station(s)
            // from the Opinet and update other fields including the carwash field in Firestore.
            case DOWNLOAD_NEAR_STATIONS:
                InnerClazz.sInstance.threadPoolExecutor.execute(
                        ((StationListTask)task).getFireStoreRunnable());
                //InnerClazz.sInstance.threadPoolExecutor.execute(stnListTask.getFireStoreRunnable());
                //msg.sendToTarget();
                break;

            // In case FireStore has no record as to a station,
            case FIRESTORE_STATION_GET_COMPLETED:
                // Save basic information of stations in FireStore
                log.i("upload station data: %s", task);
                InnerClazz.sInstance.threadPoolExecutor.execute(((StationListTask)task).setFireStoreRunnalbe());
                break;

            case FIRESTORE_STATION_SET_COMPLETED:
                log.i("upload station info to Firestore");
                msg.sendToTarget();
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
        if(geocoderTask == null) geocoderTask = new GeocoderTask(context);
        geocoderTask.initGeocoderTask(model, addrs);
        InnerClazz.sInstance.threadPoolExecutor.execute(geocoderTask.getGeocoderRunnable());
        return geocoderTask;
    }

    public GeocoderReverseTask startReverseGeocoderTask (
            Context context, LocationViewModel model, Location location) {
        //GeocoderReverseTask geocoderReverseTask = (GeocoderReverseTask)sInstance.mTaskWorkQueue.poll();
        if(geocoderReverseTask == null) geocoderReverseTask = new GeocoderReverseTask(context);
        geocoderReverseTask.initGeocoderReverseTask(model, location);
        InnerClazz.sInstance.threadPoolExecutor.execute(geocoderReverseTask.getGeocoderRunnable());
        return geocoderReverseTask;
    }

    public DistCodeSpinnerTask loadDistSpinnerTask(Context context, OpinetViewModel model, int code) {
        if(distSpinnerTak == null) distSpinnerTak = new DistCodeSpinnerTask(context);
        distSpinnerTak.initSpinnerDistCodeTask(model, code);
        InnerClazz.sInstance.threadPoolExecutor.execute(distSpinnerTak.getDistCodeSpinnerRunnable());
        return distSpinnerTak;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public GasPriceTask startGasPriceTask(
            Context context, OpinetViewModel model, String distCode, String stnId) {
        GasPriceTask gasPriceTask = InnerClazz.sInstance.mGasPriceTaskQueue.poll();

        if(gasPriceTask == null) gasPriceTask = new GasPriceTask(context);
        gasPriceTask.initPriceTask(model, distCode, stnId);
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getAvgPriceRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getSidoPriceRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getSigunPriceRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(gasPriceTask.getStationPriceRunnable());

        log.i("queue: %s", InnerClazz.sInstance.threadPoolExecutor.getQueue());

        return gasPriceTask;
    }

    public LocationTask fetchLocationTask(Context context, LocationViewModel model){
        log.i("TaskQueue: %s", InnerClazz.sInstance.mThreadTaskQueue.size());
        //locationTask = (LocationTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
        if(locationTask == null) locationTask = new LocationTask(context);
        locationTask.initLocationTask(model);
        InnerClazz.sInstance.threadPoolExecutor.execute(locationTask.getLocationRunnable());
        log.i("Location thread queue: %s", InnerClazz.sInstance.threadPoolExecutor.getQueue());
        return locationTask;
    }


    // Download stations around the current location from Opinet given the current location fetched
    // by LocationTask and defaut params transferred from OpinetStationListFragment
    public StationListTask startStationListTask(StationListViewModel model, Location location, String[] params) {
        // TEST Coding
        log.i("TaskQueue: %s", InnerClazz.sInstance.mThreadTaskQueue.size());
        /*
        for(int i = 0; i <  InnerClazz.sInstance.mThreadTaskQueue.size(); i++) {
            ThreadTask task = InnerClazz.sInstance.mThreadTaskQueue.poll();
            log.i("current task: %s", task);
            if(task instanceof StationListTask) {
                stnListTask = (StationListTask)task;
                break;
            }
        }
         */

        //stnListTask = (StationListTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
        if(stnListTask == null) {
            log.i("craete stationListTask");
            stnListTask = new StationListTask();
        }
        stnListTask.initStationTask(model, location, params);
        InnerClazz.sInstance.threadPoolExecutor.execute(stnListTask.getStationListRunnable());
        return stnListTask;
    }

    public static UploadBitmapTask startBitmapUploadTask(
            Context context, final Uri uriImage, final int position, ImageViewModel model) {

        UploadBitmapTask uploadBitmapTask = InnerClazz.sInstance.mUploadBitmapTaskQueue.poll();
        if(uploadBitmapTask == null) uploadBitmapTask = new UploadBitmapTask(context);
        uploadBitmapTask.initBitmapTask(uriImage, position, model);
        InnerClazz.sInstance.threadPoolExecutor.execute(uploadBitmapTask.getBitmapResizeRunnable());

        return uploadBitmapTask;
    }

    public static UploadPostTask uploadPostTask (
            Context context, Map<String, Object> post, FragmentSharedModel viewModel) {

        UploadPostTask uploadPostTask = InnerClazz.sInstance.mUploadPostTaskQueue.poll();
        if(uploadPostTask == null) uploadPostTask = new UploadPostTask(context);
        uploadPostTask.initPostTask(post, viewModel);
        InnerClazz.sInstance.threadPoolExecutor.execute(uploadPostTask.getUploadPostRunnable());

        return uploadPostTask;
    }



    private void recycleTask(ThreadTask task) {
        log.i("recycle task: %s", task);
        if(task instanceof GasPriceTask) {
            //gasPriceTask.recycle();
            //mGasPriceTaskQueue.offer(gasPriceTask);
        } else if(task instanceof LocationTask) {
            locationTask.recycle();
            //locationTask = null;
            mLocationTaskQueue.offer((LocationTask)task);
        } else if(task instanceof StationListTask) {
            //stnListTask.recycle();
            //stnListTask = null;
            //mStnListTaskQueue.offer((StationListTask)task);
        } else if(task instanceof UploadBitmapTask) {
            ((UploadBitmapTask)task).recycle();
            mUploadBitmapTaskQueue.offer((UploadBitmapTask)task);

        } else if(task instanceof UploadPostTask) {
            task.recycle();
            mUploadPostTaskQueue.offer((UploadPostTask)task);
        }

    }


}