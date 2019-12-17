package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.models.ServiceCenterViewModel;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.models.StationListViewModel;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadManager.class);

    // Constants
    static final int DOWNLOAD_PRICE_COMPLETED = 100;
    static final int DOWNLOAD_NEAR_STATIONS_COMPLETED = 101;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETED = 102;
    static final int DOWNLOAD_STATION_INFO_COMPLETED = 200;

    static final int DOWNLOAD_IMAGE_FINISH = 201;

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

    static final int DOWNLOAD_PRICE_FAILED = -100;
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
    private static final int CORE_POOL_SIZE = 4;
    // Sets the maximum threadpool size to 4
    private static final int MAXIMUM_POOL_SIZE = 4;
    /*
     * NOTE: This is the number of total available cores. On current versions of
     * Android, with devices that use plug-and-play cores, this will return less
     * than the total number of cores. The total number of cores is not
     * available in current Android implementations.
     */
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();


    // A queue of Runnables
    //private final BlockingQueue<Runnable> mOpinetDownloadWorkQueue, mLoadPriceWorkQueue;
    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final BlockingQueue<Runnable> mDecodeWorkQueue;

    // A queue of tasks. Tasks are handed to a ThreadPool.
    //private final Queue<ThreadTask> mThreadTaskWorkQueue;

    private Queue<ThreadTask> mTaskWorkQueue;
    private final Queue<DistrictCodeTask> mDistrictCodeTaskQueue;
    private final Queue<LoadDistCodeTask> mLoadDistCodeTaskQueue;
    private final Queue<PriceFavoriteTask> mPriceFavoriteTaskQueue;
    private final Queue<TabPagerTask> mTabPagerTaskQueue;
    private final Queue<StationListTask> mStationListTaskQueue;
    private final Queue<LocationTask> mLocationTaskQueue;
    private final Queue<PriceDistrictTask> mPriceDistrictTaskQueue;
    private final Queue<DownloadImageTask> mDownloadImageTaskQueue;

    private final Queue<ThreadTask> mBitmapResizeTask;


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

        mDistrictCodeTaskQueue = new LinkedBlockingQueue<>();
        mLoadDistCodeTaskQueue = new LinkedBlockingQueue<>();
        mPriceDistrictTaskQueue = new LinkedBlockingQueue<>();
        mTabPagerTaskQueue = new LinkedBlockingQueue<>();
        mPriceFavoriteTaskQueue = new LinkedBlockingQueue<>();
        mStationListTaskQueue = new LinkedBlockingQueue<>();

        mLocationTaskQueue = new LinkedBlockingQueue<>();
        //mClockTaskQueue = new LinkedBlockingQueue<>();
        mDownloadImageTaskQueue = new LinkedBlockingQueue<>();

        mBitmapResizeTask = new LinkedBlockingQueue<>();

        // Instantiates ThreadPoolExecutor
        //Log.i(LOG_TAG, "NUMBER_OF_CORES: " + NUMBER_OF_CORES);
        mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);


        mDecodeThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES,
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
                PriceDistrictTask priceDistrictTask;
                LocationTask locationTask;
                //LoadPriceListTask loadPriceTask;
                StationListTask stationListTask;
                StationInfoTask stationInfoTask;
                DistrictCodeTask districtCodeTask;
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
                        districtCodeTask = (DistrictCodeTask)msg.obj;
                        districtCodeTask.recycle();
                        break;


                    case DOWNLOAD_PRICE_COMPLETED:
                        priceDistrictTask = (PriceDistrictTask)msg.obj;
                        recycleTask(priceDistrictTask);

                        break;

                    case FETCH_LOCATION_COMPLETED:
                        locationTask = (LocationTask)msg.obj;
                        //recycleTask(locationTask);

                        break;

                    case FETCH_LOCATION_FAILED:
                        locationTask = (LocationTask)msg.obj;
                        //locationTask.recycle();

                        break;

                    case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                        log.i("DOWNLOAD_NEAR_STATIONS_COMPLETED");
                        break;

                    case DOWNLOAD_NEAR_STATIONS_FAILED:
                        //mStationTaskListener.onTaskFailure();
                        //recycleTask((StationListTask)msg.obj);
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

                    case DOWNLOAD_IMAGE_FINISH:
                        //recycleTask((DownloadImageTask)msg.obj);
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
    public static DistrictCodeTask saveDistrictCodeTask(Context context, OpinetViewModel model) {

        DistrictCodeTask task = sInstance.mDistrictCodeTaskQueue.poll();

        if(task == null) {
            task = new DistrictCodeTask(context, model);
        }

        sInstance.mDownloadThreadPool.execute(task.getOpinetDistCodeRunnable());

        return task;
    }

    // Retrieves Sigun list with a sido code given in SettingPreferenceActivity
    public static LoadDistCodeTask loadSpinnerDistCodeTask(
            Context context, SpinnerDistrictModel model, int code) {

        LoadDistCodeTask task = sInstance.mLoadDistCodeTaskQueue.poll();
        if(task == null) task = new LoadDistCodeTask(context);

        task.initSpinnerDistCodeTask(model, code);
        sInstance.mDecodeThreadPool.execute(task.getLoadDistCodeRunnable());

        return task;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public static PriceDistrictTask startPriceDistrictTask(
            Context context, OpinetViewModel model, String distCode, String stnId) {

        PriceDistrictTask priceDistrictTask = sInstance.mPriceDistrictTaskQueue.poll();

        if(priceDistrictTask == null) {
            priceDistrictTask = new PriceDistrictTask(context);
        }

        priceDistrictTask.initPriceTask(model, distCode, stnId);

        sInstance.mDownloadThreadPool.execute(priceDistrictTask.getAvgPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceDistrictTask.getSidoPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceDistrictTask.getSigunPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceDistrictTask.getStationPriceRunnable());

        return priceDistrictTask;
    }

    // Retrieve the price of a favorite station or service.
    // boolean isFirst indicates that when it is set to true, it is the first favorite which shows
    // the price info in the main activity as the data is saved in the cache directory.
    // Otherwise, it just fetches the price of a favorite when selected out the list.
    public static PriceFavoriteTask startFavoritePriceTask(
            Context context, OpinetViewModel model, String stnId, boolean isFirst) {

        PriceFavoriteTask stnPriceTask = (PriceFavoriteTask)sInstance.mTaskWorkQueue.poll();
        if(stnPriceTask == null) stnPriceTask = new PriceFavoriteTask(context);

        stnPriceTask.initTask(model, stnId, isFirst);
        sInstance.mDownloadThreadPool.execute(stnPriceTask.getPriceRunnableStation());

        return stnPriceTask;
    }


    public static TabPagerTask startTabPagerTask(
            Context context,
            FragmentManager fragmentManager,
            PagerAdapterViewModel model,
            String[] defaults, String jsonDistrict, String jsonSvcItem){

        TabPagerTask tabPagerTask = (TabPagerTask)sInstance.mTaskWorkQueue.poll();

        if(tabPagerTask == null) {
            tabPagerTask = new TabPagerTask(context);
        }

        tabPagerTask.initViewPagerTask(fragmentManager, model, defaults, jsonDistrict, jsonSvcItem);

        sInstance.mDecodeThreadPool.execute(tabPagerTask.getTabPagerRunnable());
        sInstance.mDecodeThreadPool.execute(tabPagerTask.getServiceItemsRunnable());

        return tabPagerTask;
    }

    public static LocationTask fetchLocationTask(Context context, LocationViewModel model){

        LocationTask locationTask = sInstance.mLocationTaskQueue.poll();

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

        StationListTask stationListTask = sInstance.mStationListTaskQueue.poll();

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

    public static GeocoderReverseTask startReverseGeocoderTask(
            Context context, LocationViewModel model, Location location) {

        GeocoderReverseTask geocoderReverseTask = (GeocoderReverseTask)sInstance.mTaskWorkQueue.poll();
        if(geocoderReverseTask == null) geocoderReverseTask = new GeocoderReverseTask(context);

        geocoderReverseTask.initGeocoderReverseTask(model, location);
        sInstance.mDecodeThreadPool.execute(geocoderReverseTask.getGeocoderRunnable());
        return geocoderReverseTask;
    }

    public static GeocoderTask startGeocoderTask(
            Context context, LocationViewModel model, String addrs) {

        GeocoderTask geocoderTask = (GeocoderTask)sInstance.mTaskWorkQueue.poll();
        if(geocoderTask == null) geocoderTask = new GeocoderTask(context);

        geocoderTask.initGeocoderTask(model, addrs);
        sInstance.mDownloadThreadPool.execute(geocoderTask.getGeocoderRunnable());

        return geocoderTask;

    }

    public static DownloadImageTask downloadImageTask(
            Context context, int position, String url, ImageViewModel model) {

        DownloadImageTask imageTask = sInstance.mDownloadImageTaskQueue.poll();
        if(imageTask == null) imageTask = new DownloadImageTask(context, model);
        imageTask.initTask(position, url);
        sInstance.mDownloadThreadPool.execute(imageTask.getDownloadImageRunnable());

        return imageTask;
    }

    // Upload the downsized user image to Firebase Storage
    public static UploadBitmapTask startBitmapUploadTask(Context context, Uri uri, ImageViewModel model) {

        ThreadTask bitmapTask = sInstance.mTaskWorkQueue.poll();
        if(bitmapTask == null) bitmapTask = new UploadBitmapTask(context);
        ((UploadBitmapTask)bitmapTask).initBitmapTask(uri, model);
        sInstance.mDownloadThreadPool.execute(((UploadBitmapTask)bitmapTask).getmBitmapResizeRunnable());

        return (UploadBitmapTask)bitmapTask;
    }

    public static AttachedBitmapTask startAttachedBitmapTask(
            Context context, List<String> imgList, ImageViewModel viewModel) {

        ThreadTask downBitmapTask = sInstance.mTaskWorkQueue.poll();
        if(downBitmapTask == null) downBitmapTask = new AttachedBitmapTask(context, viewModel);

        for(int i = 0; i < imgList.size(); i++) {
            ((AttachedBitmapTask) downBitmapTask).initTask(imgList.get(i), i);
            sInstance.mDownloadThreadPool.execute(((AttachedBitmapTask) downBitmapTask).getAttachedBitmapRunnable());
        }

        return (AttachedBitmapTask)downBitmapTask;
    }

    public static UploadPostTask startUploadPostTask(
            Context context, Map<String, Object> post, FragmentSharedModel viewModel) {

        ThreadTask postTask = sInstance.mTaskWorkQueue.poll();
        if(postTask == null) postTask = new UploadPostTask(context);
        ((UploadPostTask)postTask).initPostTask(post, viewModel);

        sInstance.mDownloadThreadPool.execute(((UploadPostTask)postTask).getUploadPostRunnable());

        return (UploadPostTask)postTask;
    }



        /*
     * Recycles tasks by calling their internal recycle() method and then putting them back into
     * the task queue.
     */
    private void recycleTask(ThreadTask task) {

        if(task instanceof PriceDistrictTask) {
            log.i("PriceDistrictTask thread: %s", task.getCurrentThread());
            if(task.getCurrentThread() != null) task.getCurrentThread().interrupt();
            mPriceDistrictTaskQueue.offer((PriceDistrictTask)task);

        } else if(task instanceof LocationTask) {
            ((LocationTask) task).recycle();
            //if(task.getCurrentThread() != null) task.getCurrentThread().interrupt();
            mLocationTaskQueue.offer((LocationTask) task);

        } else if(task instanceof StationListTask) {
            ((StationListTask) task).recycle();
            if(task.getCurrentThread() != null) task.getCurrentThread().interrupt();
            mStationListTaskQueue.offer((StationListTask)task);

            //mStationTaskListener = null;
            //if(mCurrentStationListener != null) mCurrentStationListener = null;

        } else if(task instanceof StationInfoTask) {
            ((StationInfoTask)task).recycle();
            //mStationInfoTaskQueue.offer((StationInfoTask)task);
            //mStationInfoListener = null;
        } else if(task instanceof GeocoderReverseTask) {
            ((GeocoderReverseTask)task).recycle();
            mTaskWorkQueue.offer(task);

        } else if(task instanceof DownloadImageTask) {
            mTaskWorkQueue.offer(task);
        } else {
            mTaskWorkQueue.offer(task);
        }

    }

}
