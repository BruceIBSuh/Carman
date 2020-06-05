package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.viewmodels.LocationViewModel;
import com.silverback.carman2.viewmodels.OpinetViewModel;
import com.silverback.carman2.viewmodels.PagerAdapterViewModel;
import com.silverback.carman2.viewmodels.ServiceCenterViewModel;
import com.silverback.carman2.viewmodels.SpinnerDistrictModel;
import com.silverback.carman2.viewmodels.StationListViewModel;

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
    static final int DOWNLOAD_PRICE_FAILED = -100;

    static final int DOWNLOAD_NEAR_STATIONS_COMPLETED = 101;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETED = 102;
    //static final int DOWNLOAD_STATION_INFO_COMPLETED = 103;

    static final int DOWNLOAD_IMAGE_FINISH = 201;

    static final int UPLOAD_BITMAP_COMPLETED = 1000;
    //static final int UPLOAD_BITMA_COMPLETED = 1001;

    //static final int SERVICE_ITEM_LIST_COMPLETED = 109;
    static final int FETCH_LOCATION_COMPLETED = 110;
    static final int FETCH_LOCATION_FAILED = -110;
    //static final int FETCH_ADDRESS_COMPLETED = 111;
    //static final int DOWNLOAD_DISTCODE_COMPLTETED = 112;
    //static final int LOAD_SPINNER_DIST_CODE_COMPLETE = 113;
    //static final int LOAD_SPINNER_DIST_CODE_FAILED = -113;
    static final int FIRESTORE_STATION_GET_COMPLETED = 202;
    static final int FIRESTORE_STATION_SET_COMPLETED = 203;


    //static final int RECYCLER_ADAPTER_SERVICE_COMPLETED = 140;
    //static final int RECYCLER_ADAPTER_SERVICE_FAILED = -141;

    static final int GEOCODER_REVERSE_TASK_COMPLETED = 150;
    static final int GEOCODER_REVERSE_TASK_FAILED = -150;

    //static final int DOWNLOAD_PRICE_FAILED = -100;
    static final int DOWNLOAD_NEAR_STATIONS_FAILED = -2;
    static final int DOWNLOAD_CURRENT_STATION_FAILED = -4;
    //static final int POPULATE_STATION_LIST_FAILED = -3;
    //static final int DOWNLOAD_STATION_INFO_FAILED = -5;
    //static final int FETCH_ADDRESS_FAILED = -6;
    //static final int DOWNLOAD_DISTCODE_FAILED = -7;

    //static final int SERVICE_ITEM_LIST_FAILED = -9;

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
    private final Queue<GasPriceTask> mGasPriceTaskQueue;
    private final Queue<DistCodeSpinnerTask> mDistCodeSpinnerTaskQueue;
    private final Queue<FavoritePriceTask> mFavoritePriceTaskQueue;
    private final Queue<ExpenseTabPagerTask> mExpenseTabPagerTaskQueue;
    private final Queue<StationListTask> mStationListTaskQueue;
    private final Queue<LocationTask> mLocationTaskQueue;
    private final Queue<UploadBitmapTask> mUploadBitmapTaskQueue;
    private final Queue<DownloadImageTask> mDownloadImageTaskQueue;

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
        // Runnable work queues which are used as param in ThreadPoolExecutor.execute()
        mDownloadWorkQueue = new LinkedBlockingQueue<>();
        mDecodeWorkQueue = new LinkedBlockingQueue<>();

        // Queues of tasks, which extends ThreadPool.
        mTaskWorkQueue = new LinkedBlockingQueue<>();
        //mFirestoreResQueue = new LinkedBlockingQueue<>();
        mDistrictCodeTaskQueue = new LinkedBlockingQueue<>();
        mDistCodeSpinnerTaskQueue = new LinkedBlockingQueue<>();
        mGasPriceTaskQueue = new LinkedBlockingQueue<>();
        mExpenseTabPagerTaskQueue = new LinkedBlockingQueue<>();
        mFavoritePriceTaskQueue = new LinkedBlockingQueue<>();
        mStationListTaskQueue = new LinkedBlockingQueue<>();
        mLocationTaskQueue = new LinkedBlockingQueue<>();
        mUploadBitmapTaskQueue = new LinkedBlockingQueue<>();
        mDownloadImageTaskQueue = new LinkedBlockingQueue<>();

        // Instantiates ThreadPoolExecutor
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
         * ViewModel may replace this with LiveData which send values from worker threads directly to
         * the main thread.
         */
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ThreadTask task = (ThreadTask)msg.obj;
                recycleTask(task);
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
            // StationListTask contains multiple Runnables of StationListRunnable, FirestoreGetRunnable,
            // and FirestoreSetRunnable to get the station data b/c the Opinet provides related data
            // in different URLs. This process will continue until Firetore will complete to hold up
            // the data in a unified form.

            // StationListRunnable downloads near stations or the current station from the Opinet,
            // the id(s) of which is used to check if the station(s) has been already saved in
            // Firestore. Otherwise, add the station(s) to Firestore w/ the carwash field left null.
            // Continuously, FireStoreSetRunnable downloads the additional data of the station(s)
            // from the Opinet and update other fields including the carwash field in Firestore.
            case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                mDownloadThreadPool.execute(((StationListTask)task).getFireStoreRunnable());
                //msg.sendToTarget();
                break;

            // In case FireStore has no record as to a station,
            case FIRESTORE_STATION_GET_COMPLETED:
                // Save basic information of stations in FireStore
                mDownloadThreadPool.execute(((StationListTask) task).setFireStoreRunnalbe());
                //msg.sendToTarget();
                break;
            /*
            case FIRESTORE_STATION_SET_COMPLETED:
                //msg.sendToTarget();
                break;
            */

            default:
                log.i("handle task: %s", task);
                msg.sendToTarget();
                break;
        }
    }


    @SuppressWarnings("all")
    public static synchronized void cancelAllThreads() {

        ThreadTask[] taskDownloadArray = new ThreadTask[sInstance.mDownloadWorkQueue.size()];
        ThreadTask[] taskDecodeArray = new ThreadTask[sInstance.mDecodeWorkQueue.size()];

        // Populates the array with the task objects in the queue
        sInstance.mDownloadWorkQueue.toArray(taskDownloadArray);
        sInstance.mDecodeWorkQueue.toArray(taskDecodeArray);

        // Stores the array length in order to iterate over the array
        int taskDownloadArrayLen = taskDownloadArray.length;
        int taskDecodeArrayLen = taskDecodeArray.length;

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
    public static DistrictCodeTask saveDistrictCodeTask(Context context, OpinetViewModel model) {

        DistrictCodeTask task = sInstance.mDistrictCodeTaskQueue.poll();
        if(task == null) task = new DistrictCodeTask(context, model);

        sInstance.mDownloadThreadPool.execute(task.getOpinetDistCodeRunnable());
        return task;
    }

    // Retrieves Sigun list with a sido code given in SettingPreferenceActivity
    // Bugs: no guarantee to coincide the position with the code. For example, Daegu is positioned
    // at 12, whereas the Sido code is 14.
    public static DistCodeSpinnerTask loadDistCodeSpinnerTask(
            Context context, SpinnerDistrictModel model, int position) {

        DistCodeSpinnerTask task = sInstance.mDistCodeSpinnerTaskQueue.poll();
        if(task == null) task = new DistCodeSpinnerTask(context);

        task.initSpinnerDistCodeTask(model, position);
        sInstance.mDecodeThreadPool.execute(task.getDistCodeSpinnerRunnable());

        return task;
    }

    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.
    public static GasPriceTask startGasPriceTask(
            Context context, OpinetViewModel model, String distCode, String stnId) {

        GasPriceTask gasPriceTask = sInstance.mGasPriceTaskQueue.poll();

        if(gasPriceTask == null) gasPriceTask = new GasPriceTask(context);

        gasPriceTask.initPriceTask(model, distCode, stnId);

        sInstance.mDownloadThreadPool.execute(gasPriceTask.getAvgPriceRunnable());
        sInstance.mDownloadThreadPool.execute(gasPriceTask.getSidoPriceRunnable());
        sInstance.mDownloadThreadPool.execute(gasPriceTask.getSigunPriceRunnable());
        sInstance.mDownloadThreadPool.execute(gasPriceTask.getStationPriceRunnable());

        return gasPriceTask;
    }

    // Retrieve the price of a favorite gas station. The boolean isFirst indicates that when it is
    // set to true, the gas station is the first placeholder in the favorite list, the price of which
    // is shown in GeneralFragment of MainActivity. The firstholder gas pirce is saved as the internal
    // storage and by comparing the saved price and the current price, price difference will be
    // calculated. Otherwise, it just fetches the price of a favorite when selected out the list.
    public static FavoritePriceTask startFavoritePriceTask(
            Context context, @Nullable OpinetViewModel model, String stnId, boolean isFirst) {

        FavoritePriceTask favPriceTask = sInstance.mFavoritePriceTaskQueue.poll();
        if(favPriceTask == null) favPriceTask = new FavoritePriceTask(context);

        favPriceTask.initTask(model, stnId, isFirst);
        sInstance.mDownloadThreadPool.execute(favPriceTask.getPriceRunnableStation());

        return favPriceTask;
    }


    public static ExpenseTabPagerTask startExpenseTabPagerTask(
            Context context, FragmentManager fragmentManager, PagerAdapterViewModel model,
            String[] defaults, String jsonDistrict, String jsonSvcItem){

        ExpenseTabPagerTask expenseTabPagerTask = sInstance.mExpenseTabPagerTaskQueue.poll();

        if(expenseTabPagerTask == null) {
            expenseTabPagerTask = new ExpenseTabPagerTask(context);
        }

        expenseTabPagerTask.initPagerTask(fragmentManager, model, defaults, jsonDistrict, jsonSvcItem);

        sInstance.mDecodeThreadPool.execute(expenseTabPagerTask.getTabPagerRunnable());
        sInstance.mDecodeThreadPool.execute(expenseTabPagerTask.getServiceItemsRunnable());

        return expenseTabPagerTask;
    }

    public static LocationTask fetchLocationTask(Context context, LocationViewModel model){

        LocationTask locationTask = sInstance.mLocationTaskQueue.poll();

        if(locationTask == null) {
            locationTask = new LocationTask(context);
        }

        locationTask.initLocationTask(model);
        sInstance.mDecodeThreadPool.execute(locationTask.getLocationRunnable());

        return locationTask;

    }

    // Download stations around the current location from Opinet given the current location fetched
    // by LocationTask and defaut params transferred from OpinetStationListFragment
    public static StationListTask startStationListTask(
            StationListViewModel model, Location location, String[] params) {

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

    public static DownloadImageTask downloadBitmapTask(Context context, String url, ImageViewModel model) {

        DownloadImageTask imageTask = sInstance.mDownloadImageTaskQueue.poll();
        if(imageTask == null) imageTask = new DownloadImageTask(context, model);
        imageTask.initTask(url);
        sInstance.mDownloadThreadPool.execute(imageTask.getDownloadImageRunnable());

        return imageTask;
    }

    // Upload the downsized user image to Firebase Storage
    public static UploadBitmapTask startBitmapUploadTask(
            Context context, Uri uriImage, int position, ImageViewModel model) {

        UploadBitmapTask uploadBitmapTask = sInstance.mUploadBitmapTaskQueue.poll();

        if(uploadBitmapTask == null) uploadBitmapTask = new UploadBitmapTask(context);
        else log.i("recycler task");

        uploadBitmapTask.initBitmapTask(uriImage, position, model);
        sInstance.mDownloadThreadPool.execute(uploadBitmapTask.getBitmapResizeRunnable());

        return uploadBitmapTask;
    }

    /*
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
    */

    public static UploadPostTask startUploadPostTask(
            Context context, Map<String, Object> post, FragmentSharedModel viewModel) {

        ThreadTask postTask = sInstance.mTaskWorkQueue.poll();
        if(postTask == null) postTask = new UploadPostTask(context);
        ((UploadPostTask)postTask).initPostTask(post, viewModel);

        sInstance.mDownloadThreadPool.execute(((UploadPostTask)postTask).getUploadPostRunnable());

        return (UploadPostTask)postTask;
    }


    private void recycleTask(ThreadTask task) {
        if(task instanceof LocationTask) {
            ((LocationTask)task).recycle();
            mLocationTaskQueue.offer((LocationTask)task);

        } else if(task instanceof StationListTask) {
            // Offer() should be invoked when FirestoreSetRunnable completes.
            mStationListTaskQueue.offer((StationListTask)task);

        } else if(task instanceof GasPriceTask) {
            ((GasPriceTask) task).recycle();
            mGasPriceTaskQueue.offer((GasPriceTask) task);

        } else if(task instanceof FavoritePriceTask) {
            ((FavoritePriceTask)task).recycle();
            mFavoritePriceTaskQueue.offer((FavoritePriceTask)task);

        } else if(task instanceof GeocoderReverseTask) {
            ((GeocoderReverseTask) task).recycle();
            mTaskWorkQueue.offer(task);

        } else if(task instanceof UploadBitmapTask) {
            log.i("Recycle UploadBitmapTask");
            ((UploadBitmapTask)task).recycle();
            mUploadBitmapTaskQueue.offer((UploadBitmapTask)task);

        } else if(task instanceof DownloadImageTask) {
            mTaskWorkQueue.offer(task);

        } else if(task instanceof DistCodeSpinnerTask) {
            ((DistCodeSpinnerTask)task).recycle();
            mDistCodeSpinnerTaskQueue.offer((DistCodeSpinnerTask)task);
        }

        // Interrupt the current thread if it is of no use.
        if(task.getCurrentThread() != null) {
            log.i("Interrupt the current thread: %s", task.getCurrentThread());
            task.getCurrentThread().interrupt();
        }

    }


}
