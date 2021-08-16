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
import com.silverback.carman.viewmodels.PagerAdapterViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

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

    static final int FETCH_LOCATION_FAILED = -100;
    static final int DOWNLOAD_STATION_FAILED = -101;

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
    private final ThreadPoolExecutor threadPoolExecutor;
    private final Handler mMainHandler;

    private ThreadTask threadTask;
    private DistCodeDownloadTask distCodeTask;
    private GasPriceTask gasPriceTask;
    private LocationTask locationTask;
    private StationListTask stnListTask;
    private ExpenseTabPagerTask expenseTask;

    // Constructor private
    private ThreadManager2() {
        super();
        mThreadTaskQueue = new LinkedBlockingQueue<>();
        mWorkerThreadQueue = new LinkedBlockingQueue<>();
        mStnListTaskQueue = new LinkedBlockingQueue<>(3);
        mLocationTaskQueue = new LinkedBlockingQueue<>();

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
                if(task != null) recycleTask(task);
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
                msg.sendToTarget();
                break;

            //default: msg.sendToTarget();
        }
    }


    // Download the district code from Opinet, which is fulfilled only once when the app runs first
    // time.
    public DistCodeDownloadTask saveDistrictCodeTask(Context context, OpinetViewModel model) {
        //DistCodeDownloadTask distCodeTask = (DistCodeDownloadTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
        log.i("DistcodeTask: %s", distCodeTask);
        if(distCodeTask == null) distCodeTask = new DistCodeDownloadTask(context, model);
        InnerClazz.sInstance.threadPoolExecutor.execute(distCodeTask.getOpinetDistCodeRunnable());
        log.i("queue: %s", InnerClazz.sInstance.threadPoolExecutor.getQueue());

        return distCodeTask;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public GasPriceTask startGasPriceTask(
            Context context, OpinetViewModel model, String distCode, String stnId) {
        //GasPriceTask gasPriceTask = (GasPriceTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
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

    public ExpenseTabPagerTask startExpenseTabPagerTask(PagerAdapterViewModel model, String svcItems){
            //Context context, FragmentManager fm, PagerAdapterViewModel model,
            //String[] defaults, String jsonDistrict, String jsonSvcItem){

        //ExpenseTabPagerTask expenseTask = (ExpenseTabPagerTask)InnerClazz.sInstance.mThreadTaskQueue.poll();
        if(expenseTask == null) expenseTask = new ExpenseTabPagerTask();
        //expenseTask.initPagerTask(fm, model, defaults, jsonDistrict, jsonSvcItem);
        expenseTask.initTask(model, svcItems);

        //InnerClazz.sInstance.threadPoolExecutor.execute(expenseTask.getTabPagerRunnable());
        InnerClazz.sInstance.threadPoolExecutor.execute(expenseTask.getServiceItemsRunnable());

        return expenseTask;
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

    private void recycleTask(ThreadTask task) {
        log.i("recycle task: %s", task);
        if(task instanceof LocationTask) {
            locationTask.recycle();
            mLocationTaskQueue.offer((LocationTask)task);
        } else if(task instanceof StationListTask) {
            stnListTask.recycle();
            stnListTask = null;
            //mStnListTaskQueue.offer((StationListTask)task);
        }

    }


}
