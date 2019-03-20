package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.List;
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
    static final int LOAD_PRICE_AVG_COMPLETED = 101;
    static final int LOAD_PRICE_SIDO_COMPLETED = 102;
    static final int LOAD_PRICE_SIGUN_COMPLETED = 103;
    static final int DOWNLOAD_NEAR_STATIONS_COMPLETED = 104;
    static final int DOWNLOAD_NO_STATION_COMPLETED = 105;
    static final int POPULATE_STATION_LIST_COMPLETED = 106;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETED = 107;
    static final int DOWNLOAD_STATION_INFO_COMPLETED = 108;
    static final int SERVICE_ITEM_LIST_COMPLETED = 109;
    static final int FETCH_LOCATION_COMPLETED = 110;
    //static final int FETCH_ADDRESS_COMPLETED = 111;
    static final int DOWNLOAD_DISTCODE_COMPLTETED = 112;

    static final int DOWNLOAD_PRICE_FAILED = -1;
    static final int DOWNLOAD_NEAR_STATIONS_FAILED = -2;
    static final int POPULATE_STATION_LIST_FAILED = -3;
    static final int DOWNLOAD_CURRENT_STATION_FAILED = -4;
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


    // Interface to communicate w/
    public interface OnCompleteTaskListener {
        void callbackLocation(Location result);
        void callbackStationList(List<Opinet.GasStnParcelable> result);
    }

    // Objects
    private OnCompleteTaskListener mTaskListener;

    // A queue of Runnables
    //private final BlockingQueue<Runnable> mOpinetDownloadWorkQueue, mLoadPriceWorkQueue;
    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final BlockingQueue<Runnable> mDecodeWorkQueue;

    // A queue of tasks. Tasks are handed to a ThreadPool.
    //private final Queue<ThreadTask> mThreadTaskWorkQueue;

    private final Queue<StationListTask> mStationListTaskQueue;
    private final Queue<StationInfoTask> mStationInfoTaskQueue;
    //private final Queue<StationCurrentTask> mCurStnTaskWorkQueue;
    //private final Queue<LoadPriceListTask> mLoadPriceListTaskQueue;
    //private final Queue<ServiceListTask> mServiceListTaskQueue;
    private final Queue<LocationTask> mLocationTaskQueue;

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

    // Private constructor for Singleton instance of this ThreadManager class.
    private ThreadManager() {

        // Runnable work queue
        mDownloadWorkQueue = new LinkedBlockingQueue<>();
        mDecodeWorkQueue = new LinkedBlockingQueue<>();

        // Queues of tasks, which is handed to ThreadPool.
        mStationListTaskQueue = new LinkedBlockingQueue<>();
        mStationInfoTaskQueue = new LinkedBlockingQueue<>();
        //mCurStnTaskWorkQueue = new LinkedBlockingQueue<>();
        //mServiceListTaskQueue = new LinkedBlockingQueue<>();
        mLocationTaskQueue = new LinkedBlockingQueue<>();


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
            @SuppressWarnings("unchecked")
            @Override
            public void handleMessage(Message msg) {

                //Log.d(LOG_TAG, "mMainHandler Message: " + msg.what + "," + msg.obj);

                LocationTask locationTask;
                //LoadPriceListTask loadPriceTask;
                StationListTask stationTask;
                //StationCurrentTask curStnTask;
                OpinetDistCodeTask distCodeTask;

                switch(msg.what) {
                    case DOWNLOAD_DISTCODE_COMPLTETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_DISTCODE_COMPLETED");
                        distCodeTask = (OpinetDistCodeTask)msg.obj;
                        distCodeTask.recycle();
                        break;

                    case FETCH_LOCATION_COMPLETED:
                        locationTask = (LocationTask)msg.obj;
                        Location location = locationTask.getLocationUpdated();
                        log.i("Last known location: %s, %s", location.getLongitude(), location.getLatitude());
                        mTaskListener.callbackLocation(location);

                        /*
                        } else if(act instanceof GasManagerActivity) {
                            ((GasManagerActivity)act).updateCurrentLocation(location);

                        } else if(act instanceof ServiceManagerActivity) {
                            AddFavoriteDialogFragment fm = (AddFavoriteDialogFragment)((ServiceManagerActivity) act)
                                    .getSupportFragmentManager().findFragmentByTag("dialog");

                            fm.updateCurrentLocation(location);
                        }
                        */

                        break;

                    case FETCH_LOCATION_FAILED:
                        locationTask = (LocationTask)msg.obj;
                        locationTask.recycle();
                        mLocationTaskQueue.offer(locationTask);
                        break;


                    /*
                    case DOWNLOAD_PRICE_COMPLETED:
                        //Log.i(LOG_TAG, "Download Price List Completed");
                        OpinetPriceTask downoadPriceTask = (OpinetPriceTask)msg.obj;
                        ((IntroActivity)downoadPriceTask.getParentActivity()).finishDownloadPriceList();

                        break;

                    case LOAD_PRICE_AVG_COMPLETED:
                        loadPriceTask = (LoadPriceListTask) msg.obj;
                        AvgPriceView avgView = loadPriceTask.getAvgPriceView();
                        String[] strAvg = loadPriceTask.getPriceInfo();
                        if(avgView != null) avgView.loadAvgPrice(strAvg);

                        break;

                    case LOAD_PRICE_SIDO_COMPLETED:
                        loadPriceTask = (LoadPriceListTask) msg.obj;
                        SidoPriceView sidoView = loadPriceTask.getSidoPriceView();
                        String[] strSido = loadPriceTask.getPriceInfo();
                        if(sidoView != null) sidoView.loadSidoPrice(strSido);

                        break;

                    case LOAD_PRICE_SIGUN_COMPLETED:
                        loadPriceTask = (LoadPriceListTask) msg.obj;
                        SigunPriceView sigunView = loadPriceTask.getSigunPriceView();
                        String[] strSigun = loadPriceTask.getPriceInfo();
                        if(sigunView != null) sigunView.loadSigunPrice(strSigun);

                        break;

                    case SERVICE_ITEM_LIST_COMPLETED:
                        ServiceListTask serviceTask = (ServiceListTask)msg.obj;

                        if(serviceTask == null) return;
                        serviceTask.getServiceListAdapter().notifyDataSetChanged();
                        serviceTask.getServiceListView().showListView();

                        break;

                    case DOWNLOAD_CURRENT_STATION_COMPLETED:
                        curStnTask = (StationCurrentTask)msg.obj;

                        String name = curStnTask.getCurrentStation().getStnName();
                        float price = curStnTask.getCurrentStation().getStnPrice();
                        curStnTask.getGasManagerActivity().loadStationInfo(name, price);

                        break;

                    case DOWNLOAD_CURRENT_STATION_FAILED:
                        curStnTask = (StationCurrentTask)msg.obj;
                        curStnTask.getGasManagerActivity().inputStationInfo();

                        curStnTask.recycle();
                        mCurStnTaskWorkQueue.offer(curStnTask);
                        break;


                    case DOWNLOAD_STATION_INFO_COMPLETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_STATION_INFO_COMPLETED");

                        curStnTask = (StationCurrentTask)msg.obj;

                        String addrs = curStnTask.getStationAddrs();
                        String id = curStnTask.getStationId();
                        String code = curStnTask.getStationCode();

                        curStnTask.getGasManagerActivity().addStationInfo(id, addrs, code);

                        break;
                    */
                    case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                        log.i("DOWNLOAD_NEAR_STATION_COMPLETED");
                        stationTask = (StationListTask)msg.obj;
                        List<Opinet.GasStnParcelable> stnList = stationTask.getStationList();

                        for(int i = 0; i < stnList.size(); i++) {
                            log.i("Station ID: %s", stnList.get(i).getStnId());
                            startStationInfoTask(stnList.get(i));
                        }
                        //mTaskListener.callbackStationList(stations);
                        break;

                    /*
                    case DOWNLOAD_NO_STATION_COMPLETED: // No sation available within a given radius
                        //Log.i(LOG_TAG, "DOWNLOAD NO STATION WITHIN RADIUS");
                        stationTask = (StationListTask)msg.obj;
                        stationTask.getStationListView().showStationListView(View.VISIBLE);
                        stationTask.getStationListFragment().setStationList(null);

                        stationTask.recycle();
                        mStationListTaskQueue.offer(stationTask);
                        break;

                    case DOWNLOAD_NEAR_STATIONS_FAILED:
                        //Log.i(LOG_TAG, "DOWNLOAD NEAR STATION FAILED");
                        stationTask = (StationListTask)msg.obj;
                        stationTask.getStationListView().showStationListView(View.VISIBLE);
                        stationTask.getStationListFragment().setStationList(null);

                        stationTask.recycle();
                        mStationListTaskQueue.offer(stationTask);
                        break;

                    case POPULATE_STATION_LIST_COMPLETED:
                        //Log.i(LOG_TAG, "POPULATE_STATION_LIST_COMPLETED");
                        stationTask = (StationListTask)msg.obj;

                        OpinetStationListFragment fragment = stationTask.getStationListFragment();
                        StationListView listView = stationTask.getStationListView();
                        StationListAdapter adapter = stationTask.getStationListAdapter();

                        fragment.setStationList(stationTask.getStationList());
                        adapter.notifyDataSetChanged();
                        listView.setAdapter(adapter);
                        listView.showStationListView(View.VISIBLE);

                        break;

                    case POPULATE_STATION_LIST_FAILED:
                        //Log.i(LOG_TAG, "POPULATE_STATION_LIST_FAILED");
                        stationTask = (StationListTask)msg.obj;
                        stationTask.recycle();
                        mStationListTaskQueue.offer(stationTask);
                        break;

                    case DOWNLOAD_DISTCODE_COMPLTETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_DISTCODE_COMPLETED");
                        distCodeTask = (OpinetDistCodeTask)msg.obj;
                        distCodeTask.recycle();
                        break;
                    */
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

        if(task instanceof OpinetDistCodeTask) {
            switch(state) {
                case DOWNLOAD_DISTCODE_COMPLTETED: msg.sendToTarget(); break;
                case DOWNLOAD_DISTCODE_FAILED: msg.sendToTarget(); break;
            }

        } else if(task instanceof LocationTask) {
            switch (state) {
                case FETCH_LOCATION_COMPLETED: msg.sendToTarget(); break;
                case FETCH_LOCATION_FAILED: msg.sendToTarget(); break;
            }

        } else if(task instanceof StationListTask) {

            switch (state) {

                case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                    /*
                    List<Opinet.GasStnParcelable> stnList = ((StationListTask)task).getStationList();
                    log.i("DOWNLOAD_NEAR_STATIONS_COMPLETED: %s", stnList.size());

                    for(int i = 0; i < stnList.size(); i++) {
                        ((StationListTask) task).setStationId(stnList.get(i).getStnId());
                        //mDownloadThreadPool.execute(((StationListTask) task).getStationInfoRunnable());
                    }
                    */
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NO_STATION_COMPLETED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NEAR_STATIONS_FAILED:
                    msg.sendToTarget();
                    break;
            }
        }


        /*
        } else if(task instanceof OpinetPriceTask) {

            switch(state) {
                case DOWNLOAD_PRICE_COMPLETED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_PRICE_FAILED:
                    break;
            }


        } else if(task instanceof LoadPriceListTask) {

            switch (state) {
                case LOAD_PRICE_AVG_COMPLETED:
                    msg.sendToTarget();
                    break;
                case LOAD_PRICE_SIDO_COMPLETED:
                    msg.sendToTarget();
                    break;
                case LOAD_PRICE_SIGUN_COMPLETED:
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof ServiceListTask) {

            switch(state) {
                case SERVICE_ITEM_LIST_COMPLETED:
                    msg.sendToTarget();
                    break;
                default:
                    break;
            }

        } else if(task instanceof StationListTask) {

            switch (state) {

                case DOWNLOAD_NEAR_STATIONS_COMPLETED:
                    mDownloadThreadPool.execute(((StationListTask) task).getStationListRunnable());
                    msg.sendToTarget(); //pass the downloaded list back to OpinetStationListFragment
                    break;

                case DOWNLOAD_NO_STATION_COMPLETED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NEAR_STATIONS_FAILED:
                    msg.sendToTarget();
                    break;

                case POPULATE_STATION_LIST_COMPLETED:
                    msg.sendToTarget();
                    break;

                case POPULATE_STATION_LIST_FAILED:
                    //mDownloadThreadPool.execute(((StationListTask) task).getStationListRunnable());
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof StationCurrentTask) {

            switch(state) {
                case DOWNLOAD_CURRENT_STATION_COMPLETED:
                    mDownloadThreadPool.execute(((StationCurrentTask) task).getStationInfoRunnable());
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NO_STATION_COMPLETED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_CURRENT_STATION_FAILED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_STATION_INFO_COMPLETED:
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof OpinetDistCodeTask) {

            switch(state) {
                case DOWNLOAD_DISTCODE_COMPLTETED:
                    msg.sendToTarget();
                    break;
                case DOWNLOAD_DISTCODE_FAILED:
                    msg.sendToTarget();
                    break;
            }
        }

        */


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
    public static OpinetDistCodeTask startOpinetDistCodeTask (Context context) {

        OpinetDistCodeTask task = (OpinetDistCodeTask)sInstance.mDownloadWorkQueue.poll();

        if(task == null) {
            task = new OpinetDistCodeTask(context);
        }

        sInstance.mDownloadThreadPool.execute(task.getOpinetDistCodeRunnable());

        return task;
    }


    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.

    public static OpinetPriceTask startOpinetPriceTask(Activity activity, String distCode, int sort) {

        log.i("OpinetPriceTask: %s, %s, %d", activity, distCode, sort);
        OpinetPriceTask priceTask = (OpinetPriceTask)sInstance.mDownloadWorkQueue.poll();

        if(priceTask == null) {
            priceTask = new OpinetPriceTask(activity);
        }

        priceTask.initOpinetPriceTask(ThreadManager.sInstance, activity, distCode, sort);
        sInstance.mDownloadThreadPool.execute(priceTask.getOpinetPriceListRunnable());

        return priceTask;
    }

    /*

    // Gets the average, sido, sigun oil price from the saved files
    public static LoadPriceListTask startLoadPriceTask(TextView view, String fuelCode, Uri uri){

        //LoadPriceListTask loadPriceTask = sInstance.mLoadPriceListWorkQueue.poll();
        LoadPriceListTask loadPriceTask = sInstance.mLoadPriceListTaskQueue.poll();

        if(loadPriceTask == null) {
            loadPriceTask = new LoadPriceListTask(view.getContext());
        }

        loadPriceTask.initLoadPriceTask(ThreadManager.sInstance, view, fuelCode, uri);
        sInstance.mDownloadThreadPool.execute(loadPriceTask.getLoadPriceListRunnable());

        return loadPriceTask;
    }

    // Thread to start the Service Checklist items with params of adapter, view, itemName, position
    public static ServiceListTask startServiceListTask(
            ServiceListAdapter adapter, ServiceListView view, String item, int position) {

        ServiceListTask serviceListTask = sInstance.mServiceListTaskQueue.poll();

        if(serviceListTask == null) {
            serviceListTask = new ServiceListTask(view.getContext());
        }

        serviceListTask.initServiceTask(adapter, view, item, position);
        sInstance.mDownloadThreadPool.execute(serviceListTask.getServiceItemListRunnable());

        return serviceListTask;
    }

    // Find a gas station within Constant.MIN_RADIUS distance
    public static StationCurrentTask startCurrentStationTask(
            GasManagerActivity activity, String[] defaults, Location location) {

        //CurrentStationTask task = (CurrentStationTask)sInstance.mThreadTaskWorkQueue.poll();
        StationCurrentTask task = sInstance.mCurStnTaskWorkQueue.poll();

        if(task == null) {
            //task = new CurrentStationTask(activity);
            task = new StationCurrentTask(activity.getApplicationContext());
        }

        task.initCurrentStationTask(ThreadManager.sInstance, activity, defaults, location);
        sInstance.mDownloadThreadPool.execute(task.getDownloadRunnable());

        return task;
    }

    */
    // Download stations around the current location from Opinet
    // given Location and defaut params transferred from OpinetStationListFragment
    public static StationListTask startStationListTask(Context context, String[] params, Location location) {

        StationListTask stationTask = sInstance.mStationListTaskQueue.poll();
        if(stationTask == null) {
            stationTask = new StationListTask(context);
        }

        // Attach OnCompleteTaskListener
        if(sInstance.mTaskListener == null) {
            try {
                sInstance.mTaskListener = (OnCompleteTaskListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnCompleteTaskListener");
            }
        }

        stationTask.initDownloadListTask(ThreadManager.sInstance, params, location);
        sInstance.mDownloadThreadPool.execute(stationTask.getStationListRunnable());

        return stationTask;
    }

    public static StationInfoTask startStationInfoTask(Opinet.GasStnParcelable station) {

        StationInfoTask stationInfoTask = sInstance.mStationInfoTaskQueue.poll();

        if(stationInfoTask == null) {
            stationInfoTask = new StationInfoTask();
        }

        stationInfoTask.initDownloadInfoTask(ThreadManager.sInstance, station);
        sInstance.mDecodeThreadPool.execute(stationInfoTask.getStationInfoRunnable());

        return stationInfoTask;
    }

    /*
    public static StationListTask sortNearStationsTask(
            OpinetStationListFragment fm, StationListView view, List<Opinet.GasStnParcelable> stationList) {

        StationListTask stationListTask = sInstance.mStationListTaskQueue.poll();

        if(stationListTask == null) {
            stationListTask = new StationListTask(view.getContext());
        }

        stationListTask.initSortTask(ThreadManager.sInstance, fm, view, stationList);
        sInstance.mDownloadThreadPool.execute(stationListTask.getStationListRunnable());

        return stationListTask;
    }

    */

    @SuppressWarnings("unchecked")
    public static LocationTask fetchLocationTask(Fragment fm, View view){

        LocationTask locationTask = sInstance.mLocationTaskQueue.poll();

        // Attach OnCompleteTaskListener
        try {
            sInstance.mTaskListener = (OnCompleteTaskListener)fm;
        } catch(ClassCastException e) {
            throw new ClassCastException(fm.toString() + " must implement OnCompleteTaskListener");
        }

        if(locationTask == null) {
            locationTask = new LocationTask(view);
        }

        locationTask.initLocationTask(ThreadManager.sInstance, view);
        sInstance.mDownloadThreadPool.execute(locationTask.getLocationRunnable());

        return locationTask;

    }


    /*
    public static void startBitmapTask(Context context, Uri imageUri, int width, int height) {

        CoverImageDecodeTask decodeTask = (CoverImageDecodeTask)sInstance.mThreadTaskWorkQueue.poll();

        if(decodeTask == null) {
            decodeTask = new CoverImageDecodeTask(context);
        }

        decodeTask.initImageDecodeTask(ThreadManager.sInstance, imageUri, width, height);
        sInstance.mDownloadThreadPool.execute(decodeTask.getImageDecodeRunnable());
    }
    */

}