package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.IntroActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.models.ServiceCenterViewModel;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.models.StationListViewModel;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadManager.class);

    // Constants
    static final int DOWNLOAD_PRICE_COMPLETE = 100;
    static final int DOWNLOAD_NEAR_STATIONS_COMPLETED = 101;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETED = 102;
    static final int DOWNLOAD_STATION_INFO_COMPLETED = 200;

    static final int SERVICE_ITEM_LIST_COMPLETED = 109;
    static final int FETCH_LOCATION_COMPLETED = 110;
    //static final int FETCH_ADDRESS_COMPLETED = 111;
    static final int DOWNLOAD_DISTCODE_COMPLTETED = 112;
    static final int LOAD_SPINNER_DIST_CODE_COMPLETE = 113;
    static final int UPDATE_CLOCK = 114;
    static final int LOAD_SPINNER_DIST_CODE_FAILED = -113;

    static final int FIRESTORE_STATION_GET_COMPLETED = 120;
    static final int FIRESTORE_STATION_SET_COMPLETED = 130;

    static final int RECYCLER_ADAPTER_SERVICE_COMPLETED = 140;
    static final int RECYCLER_ADAPTER_SERVICE_FAILED = -141;

    static final int GEOCODER_REVERSE_TASK_COMPLETED = 150;
    static final int GEOCODER_REVERSE_TASK_FAILED = -150;

    static final int DOWNLOAD_AVG_PRICE_COMPLETED = 201;
    static final int DOWNLOAD_SIDO_PRICE_COMPLETED = 202;
    static final int DOWNLOAD_SIGUN_PRICE_COMPLETED = 203;

    static final int DOWNLOAD_PRICE_FAILED = -200;
    static final int DOWNLOAD_NEAR_STATIONS_FAILED = -2;
    static final int DOWNLOAD_CURRENT_STATION_FAILED = -4;
    static final int POPULATE_STATION_LIST_FAILED = -3;
    static final int DOWNLOAD_STATION_INFO_FAILED = -5;
    //static final int FETCH_ADDRESS_FAILED = -6;
    static final int DOWNLOAD_DISTCODE_FAILED = -7;
    static final int FETCH_LOCATION_FAILED = -8;
    static final int SERVICE_ITEM_LIST_FAILED = -9;

    // Determine the threadpool parameters.
    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    // Sets the initial threadpool size to 4
    private static final int CORE_POOL_SIZE = 2;
    // Sets the maximum threadpool size to 4
    //private static final int MAXIMUM_POOL_SIZE = 4;
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();


    // A queue of Runnables
    //private final BlockingQueue<Runnable> mOpinetDownloadWorkQueue, mLoadPriceWorkQueue;
    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final BlockingQueue<Runnable> mDecodeWorkQueue;

    // A queue of tasks. Tasks are handed to a ThreadPool.
    //private final Queue<ThreadTask> mThreadTaskWorkQueue;

    private final Queue<StationListTask> mStationListTaskQueue;
    //private final Queue<StationInfoTask> mStationInfoTaskQueue;
    private final Queue<LocationTask> mLocationTaskQueue;
    //private final Queue<ClockTask> mClockTaskQueue;
    private final Queue<ThreadTask> mTaskWorkQueue;

    // A managed pool of background download threads
    private final ThreadPoolExecutor mDownloadThreadPool;
    private final ThreadPoolExecutor mDecodeThreadPool;

    // An object that manages Messages in a Thread
    private Handler mMainHandler;

    // ThreadManager instance as a singleton
    private static ThreadManager sInstance;

    // A static block that sets class fields
    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS; //The time unit for "keep alive" is in seconds
        sInstance = new ThreadManager();//Creates a single static instance of ThreadManager
    }

    // Private constructor for Singleton instance of ThreadManager
    private ThreadManager() {

        // Runnable work queues
        mDownloadWorkQueue = new LinkedBlockingQueue<>();
        mDecodeWorkQueue = new LinkedBlockingQueue<>();

        // Queues of tasks, which is handed to ThreadPool.
        mTaskWorkQueue = new LinkedBlockingQueue<>();

        mStationListTaskQueue = new LinkedBlockingQueue<>();
        //mStationInfoTaskQueue = new LinkedBlockingQueue<>();
        mLocationTaskQueue = new LinkedBlockingQueue<>();
        //mClockTaskQueue = new LinkedBlockingQueue<>();


        // Instantiates ThreadPoolExecutor
        //Log.i(LOG_TAG, "NUMBER_OF_CORES: " + NUMBER_OF_CORES);
        mDownloadThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);


        mDecodeThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDecodeWorkQueue);


        /*
         * Instantiates a new anonymous Handler object and defines its
         * handleMessage() method. The Handler *must* run on the UI thread, because it moves photo
         * Bitmaps from the PhotoTask object to the View object.
         * To force the Handler to run on the UI thread, it's defined as part of the PhotoManager
         * constructor. The constructor is invoked when the class is first referenced, and that
         * happens when the View invokes startDownload. Since the View runs on the UI Thread, so
         * does the constructor and the Handler.
         */

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //Log.d(LOG_TAG, "mMainHandler Message: " + msg.what + "," + msg.obj);
                ClockTask clockTask;
                PriceTask priceTask;
                LocationTask locationTask;
                //LoadPriceListTask loadPriceTask;
                StationListTask stationListTask;
                StationInfoTask stationInfoTask;
                SaveDistCodeTask saveDistCodeTask;
                LoadDistCodeTask loadDistCodeTask;

                switch(msg.what) {
                    case UPDATE_CLOCK:
                        clockTask = (ClockTask)msg.obj;
                        TextView tvDate = (TextView)clockTask.getClockView();
                        tvDate.setText(clockTask.getCurrentTime());
                        //clockTask.recycle();
                        break;
                    case DOWNLOAD_DISTCODE_COMPLTETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_DISTCODE_COMPLETED");
                        saveDistCodeTask = (SaveDistCodeTask)msg.obj;
                        saveDistCodeTask.recycle();
                        break;

                    case DOWNLOAD_PRICE_COMPLETE:
                        priceTask = (PriceTask)msg.obj;
                        // Each callback method according to the caller activity.
                        if(priceTask.getParentActivity() instanceof IntroActivity) {
                            ((IntroActivity)priceTask.getParentActivity()).onPriceTaskComplete();
                        } else if(priceTask.getParentActivity() instanceof SettingPreferenceActivity) {
                            ((SettingPreferenceActivity) priceTask.getParentActivity()).onPriceTaskComplete();
                        }

                        break;

                    case FETCH_LOCATION_COMPLETED:
                        locationTask = (LocationTask)msg.obj;
                        recycleTask(locationTask);

                        break;

                    case FETCH_LOCATION_FAILED:
                        locationTask = (LocationTask)msg.obj;
                        locationTask.recycle();

                        break;

                    case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                        log.i("DOWNLOAD_NEAR_STATIONS_COMPLETED");
                        break;

                    case DOWNLOAD_NEAR_STATIONS_FAILED:
                        //mStationTaskListener.onTaskFailure();
                        recycleTask((StationListTask)msg.obj);
                        break;

                    case DOWNLOAD_CURRENT_STATION_FAILED:
                        break;

                    case FIRESTORE_STATION_SET_COMPLETED:
                        recycleTask((StationListTask)msg.obj);
                        break;

                    case DOWNLOAD_CURRENT_STATION_COMPLETED:
                        stationListTask = (StationListTask)msg.obj;
                        recycleTask(stationListTask);
                        break;

                    case DOWNLOAD_STATION_INFO_COMPLETED:
                        stationInfoTask = (StationInfoTask)msg.obj;
                        recycleTask(stationInfoTask);
                        break;

                    case DOWNLOAD_STATION_INFO_FAILED:
                        recycleTask((StationInfoTask)msg.obj);
                        break;

                    case GEOCODER_REVERSE_TASK_COMPLETED:
                        log.i("GeocoderReverseTask completed");
                        recycleTask((GeocoderReverseTask)msg.obj);
                        break;

                    case GEOCODER_REVERSE_TASK_FAILED:
                        log.i("GeocoderReverseTask Failed");
                        recycleTask((GeocoderReverseTask)msg.obj);
                        break;

                    default:
                        // Otherwise, calls the super method
                        super.handleMessage(msg);

                }

            }

        };
    }

    // Get Singleton ThreadManager instance
    static ThreadManager getInstance() {
        return sInstance;
    }


    // Handles state messages for a particular task object
    void handleState(ThreadTask task, int state) {

        Message msg = mMainHandler.obtainMessage(state, task);

        switch(state) {
            case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                //List<Opinet.GasStnParcelable> stnList = ((StationListTask)task).getStationList();
                mDownloadThreadPool.execute(((StationListTask)task).getFireStoreRunnable());
                //msg.sendToTarget();
                break;

            // In case FireStore has no record as to a station,
            case FIRESTORE_STATION_GET_COMPLETED:
                // Save basic information of stations in FireStore
                mDecodeThreadPool.execute(((StationListTask) task).setFireStoreRunnalbe());
                //mDecodeThreadPool.execute(((StationListTask) task).getStationInfoRunnable());
                //msg.sendToTarget();
                break;

            case DOWNLOAD_STATION_INFO_COMPLETED:
                // Save additional information of a selected station in FireStore
                //mDecodeThreadPool.execute(((StationInfoTask)task).updateFireStoreRunnable());
                msg.sendToTarget();
                break;


            case RECYCLER_ADAPTER_SERVICE_COMPLETED:
                /*
                SparseArray<String> sparseSvcItemArray = ((ServiceRecyclerTask)task).getSparseServiceItemArray();
                for(int i = 0; i < sparseSvcItemArray.size(); i++) {
                    startServiceProgressTask(i);
                }

                //mDecodeThreadPool.execute(((ServiceRecyclerTask)task).getProgbarAnimRunnable());
                */
                msg.sendToTarget();
                break;

            default:
                msg.sendToTarget();
        }
    }


    @SuppressWarnings("all")
    public static synchronized void cancelAllThreads() {

        ThreadTask[] taskArray = new ThreadTask[sInstance.mDownloadWorkQueue.size()];

        // Populates the array with the task objects in the queue

        sInstance.mDownloadWorkQueue.toArray(taskArray);

        // Stores the array length in order to iterate over the array
        int taskArraylen = taskArray.length;

        //synchronized (sInstance) {
        // Iterates over the array of tasks
        for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {
            // Gets the task's current thread
            Thread thread = taskArray[taskArrayIndex].mThreadThis;

            // if the Thread exists, post an interrupt to it
            if (null != thread) {
                thread.interrupt();
            }
        }
        //}
    }


    // Download the district code from Opinet, which is fulfilled only once when the app runs first
    // time.
    public static SaveDistCodeTask downloadOpinetDistCodeTask(Context context) {

        SaveDistCodeTask task = (SaveDistCodeTask)sInstance.mTaskWorkQueue.poll();

        if(task == null) {
            task = new SaveDistCodeTask(context);
        }

        sInstance.mDownloadThreadPool.execute(task.getOpinetDistCodeRunnable());

        return task;
    }

    // Retrieves Sigun list with a sido code given in SettingPreferenceActivity
    public static LoadDistCodeTask loadSpinnerDistCodeTask(
            Context context, SpinnerDistrictModel model, int code) {

        LoadDistCodeTask task = (LoadDistCodeTask)sInstance.mTaskWorkQueue.poll();
        if(task == null) task = new LoadDistCodeTask(context);

        task.initSpinnerDistCodeTask(model, code);
        sInstance.mDecodeThreadPool.execute(task.getLoadDistCodeRunnable());

        return task;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public static PriceTask startPriceTask(Activity activity, String distCode) {

        PriceTask priceTask = (PriceTask)sInstance.mTaskWorkQueue.poll();

        if(priceTask == null) {
            priceTask = new PriceTask(activity);
        }

        priceTask.initPriceTask(ThreadManager.sInstance, activity, distCode);
        sInstance.mDownloadThreadPool.execute(priceTask.getAvgPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceTask.getSidoPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceTask.getSigunPriceRunnable());

        return priceTask;
    }


    public static TabPagerTask startViewPagerTask(
            PagerAdapterViewModel model, FragmentManager fragmentManager, String[] defaults, String json){

        TabPagerTask tabPagerTask = (TabPagerTask)sInstance.mTaskWorkQueue.poll();

        if(tabPagerTask == null) {
            tabPagerTask = new TabPagerTask();
        }

        tabPagerTask.initViewPagerTask(model, fragmentManager, defaults, json);
        sInstance.mDecodeThreadPool.execute(tabPagerTask.getViewPagerRunnable());

        return tabPagerTask;
    }

    public static LocationTask fetchLocationTask(Context context, LocationViewModel model){

        LocationTask locationTask = (LocationTask)sInstance.mTaskWorkQueue.poll();

        if(locationTask == null) {
            locationTask = new LocationTask(context);
        }

        locationTask.initLocationTask(model);
        sInstance.mDownloadThreadPool.execute(locationTask.getLocationRunnable());
        return locationTask;

    }


    // Download stations around the current location from Opinet
    // given Location and defaut params transferred from OpinetStationListFragment
    public static StationListTask startStationListTask(
            Context context, StationListViewModel model, Location location, String[] params) {

        StationListTask stationListTask = (StationListTask)sInstance.mTaskWorkQueue.poll();

        if(stationListTask == null) {
            stationListTask = new StationListTask();
        }

        stationListTask.initStationTask(model, location, params);
        sInstance.mDownloadThreadPool.execute(stationListTask.getStationListRunnable());

        return stationListTask;
    }

    // Locate a service center within a specific area with the current location and a geofence
    // matched.
    public static ServiceCenterTask startServiceCenterTask(
            Context context, ServiceCenterViewModel model, Location location) {

        ServiceCenterTask serviceCenterTask = (ServiceCenterTask)sInstance.mTaskWorkQueue.poll();
        if(serviceCenterTask == null) serviceCenterTask = new ServiceCenterTask(context);

        serviceCenterTask.initServiceTask(model, location);
        sInstance.mDownloadThreadPool.execute(serviceCenterTask.getServiceCenterRunnable());

        return serviceCenterTask;

    }

    public static GeocoderReverseTask startReverseGeocoderTask(Context context, LocationViewModel model, Location location) {
        log.i("ReverseGeocoderTask initiated");
        GeocoderReverseTask geocoderReverseTask = (GeocoderReverseTask)sInstance.mTaskWorkQueue.poll();
        if(geocoderReverseTask == null) geocoderReverseTask = new GeocoderReverseTask(context);

        geocoderReverseTask.initGeocoderReverseTask(model, location);
        sInstance.mDecodeThreadPool.execute(geocoderReverseTask.getGeocoderRunnable());
        return geocoderReverseTask;
    }

    public static GeocoderTask startGeocoderTask(Context context, LocationViewModel model, String addrs) {
        GeocoderTask geocoderTask = (GeocoderTask)sInstance.mTaskWorkQueue.poll();
        if(geocoderTask == null) geocoderTask = new GeocoderTask(context);

        geocoderTask.initGeocoderTask(model, addrs);
        sInstance.mDownloadThreadPool.execute(geocoderTask.getGeocoderRunnable());

        return geocoderTask;

    }

    public static ServiceRecyclerTask startServiceRecyclerTask (PagerAdapterViewModel model, String json) {

        ServiceRecyclerTask recyclerTask = (ServiceRecyclerTask)sInstance.mTaskWorkQueue.poll();
        if(recyclerTask == null) {
            recyclerTask = new ServiceRecyclerTask();
        }

        recyclerTask.initTask(model, json);
        sInstance.mDecodeThreadPool.execute(recyclerTask.getServiceRecyclerRunnable());

        return recyclerTask;
    }

    /*
     * Recycles tasks by calling their internal recycle() method and then putting them back into
     * the task queue.
     */
    private void recycleTask(ThreadTask task) {

        if(task instanceof LocationTask) {
            ((LocationTask) task).recycle();
            mLocationTaskQueue.offer((LocationTask) task);

        } else if(task instanceof StationListTask) {
            ((StationListTask) task).recycle();
            mStationListTaskQueue.offer((StationListTask) task);
            //mStationTaskListener = null;
            //if(mCurrentStationListener != null) mCurrentStationListener = null;

        } else if(task instanceof StationInfoTask) {
            ((StationInfoTask)task).recycle();
            //mStationInfoTaskQueue.offer((StationInfoTask)task);
            //mStationInfoListener = null;
        } else if(task instanceof GeocoderReverseTask) {
            ((GeocoderReverseTask)task).recycle();
            mTaskWorkQueue.offer(task);
        }

    }

}
