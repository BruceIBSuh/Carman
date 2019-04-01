package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.silverback.carman2.IntroActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.views.SpinnerDialogPreference;
import com.silverback.carman2.views.StationRecyclerView;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.Fragment;

public class ThreadManager {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadManager.class);

    // Constants
    static final int DOWNLOAD_PRICE_COMPLETE = 100;
    static final int DOWNLOAD_STATION_LIST_COMPLETE = 104;
    static final int DOWNLOAD_NO_STATION_COMPLETE = 105;
    static final int DOWNLOAD_STATION_INFO_COMPLETE = 106;
    static final int POPULATE_STATION_LIST_COMPLETED = 106;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETED = 107;
    static final int DOWNLOAD_STATION_INFO_COMPLETED = 108;
    static final int SERVICE_ITEM_LIST_COMPLETED = 109;
    static final int FETCH_LOCATION_COMPLETED = 110;
    //static final int FETCH_ADDRESS_COMPLETED = 111;
    static final int DOWNLOAD_DISTCODE_COMPLTETED = 112;
    static final int LOAD_SPINNER_DIST_CODE_COMPLETE = 113;
    static final int LOAD_SPINNER_DIST_CODE_FAILED = -113;

    static final int DOWNLOAD_AVG_PRICE_COMPLETED = 201;
    static final int DOWNLOAD_SIDO_PRICE_COMPLETED = 202;
    static final int DOWNLOAD_SIGUN_PRICE_COMPLETED = 203;
    static final int DOWNLOAD_PRICE_FAILED = -200;
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
        void onLocationFetched(Location result);
        void onStationInfoList(List<Opinet.GasStnParcelable> result);
        void onTaskFailure();
    }

    // Objects
    private OnCompleteTaskListener mTaskListener;

    // A queue of Runnables
    //private final BlockingQueue<Runnable> mOpinetDownloadWorkQueue, mLoadPriceWorkQueue;
    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final BlockingQueue<Runnable> mDecodeWorkQueue;

    // A queue of tasks. Tasks are handed to a ThreadPool.
    //private final Queue<ThreadTask> mThreadTaskWorkQueue;

    private final Queue<StationTask> mStationTaskQueue;
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

    private int count = 0;


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
        mStationTaskQueue = new LinkedBlockingQueue<>();
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
                PriceTask priceTask;
                LocationTask locationTask;
                //LoadPriceListTask loadPriceTask;
                StationTask stationTask;
                //StationCurrentTask curStnTask;
                DistCodeTask distCodeTask;
                SpinnerDistCodeTask spinnerDistCodeTask;



                switch(msg.what) {
                    case DOWNLOAD_DISTCODE_COMPLTETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_DISTCODE_COMPLETED");
                        distCodeTask = (DistCodeTask)msg.obj;
                        distCodeTask.recycle();
                        break;

                    case DOWNLOAD_PRICE_COMPLETE:
                        priceTask = (PriceTask)msg.obj;
                        // Notifies the caller(IntroActivity) of the 3 prices retrieved.
                        ((IntroActivity)priceTask.getParentActivity()).onPriceComplete();
                        break;

                    case FETCH_LOCATION_COMPLETED:
                        locationTask = (LocationTask)msg.obj;
                        Location location = locationTask.getLocationUpdated();
                        log.i("Last known location: %s, %s", location.getLongitude(), location.getLatitude());
                        mTaskListener.onLocationFetched(location);

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

                    case LOAD_SPINNER_DIST_CODE_COMPLETE:
                        spinnerDistCodeTask = (SpinnerDistCodeTask)msg.obj;

                        SpinnerDialogPreference pref = spinnerDistCodeTask.getDialogPreference();
                        pref.getSigunAdapter().notifyDataSetChanged();
                        spinnerDistCodeTask.recycle();

                        break;


                    /*
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


                    case DOWNLOAD_STATION_INFO_COMPLETE:
                        //Log.i(LOG_TAG, "DOWNLOAD_STATION_INFO_COMPLETE");

                        curStnTask = (StationCurrentTask)msg.obj;

                        String addrs = curStnTask.getStationAddrs();
                        String id = curStnTask.getStationId();
                        String code = curStnTask.getStationCode();

                        curStnTask.getGasManagerActivity().initStationInfo(id, addrs, code);

                        break;
                    */
                    case DOWNLOAD_STATION_LIST_COMPLETE:
                        log.i("DOWNLOAD_NEAR_STATION_COMPLETED");

                        stationTask = (StationTask)msg.obj;
                        StationRecyclerView localView = stationTask.getRecyclerView();


                        List<Opinet.GasStnParcelable> stnList = stationTask.getStationList();
                        localView.setNearStationList(stnList);
                        //mTaskListener.onStationInfoList(stations);

                        break;

                    case DOWNLOAD_NO_STATION_COMPLETE:
                        mTaskListener.onTaskFailure();
                        break;

                    case DOWNLOAD_STATION_INFO_COMPLETE:
                        log.i("DOWNLOAD_STATION_INFO_COMPLETE");
                        /*
                        stationTask = (StationTask)msg.obj;
                        List<Opinet.GasStnParcelable> stnList = stationTask.getStationInfoList();
                        mTaskListener.onStationInfoList(stnList);
                        */
                        break;

                    /*
                    case DOWNLOAD_NO_STATION_COMPLETE: // No sation available within a given radius
                        //Log.i(LOG_TAG, "DOWNLOAD NO STATION WITHIN RADIUS");
                        stationTask = (StationTask)msg.obj;
                        stationTask.getStationListView().showStationListView(View.VISIBLE);
                        stationTask.getStationListFragment().setStationList(null);

                        stationTask.recycle();
                        mStationTaskQueue.offer(stationTask);
                        break;

                    case DOWNLOAD_NEAR_STATIONS_FAILED:
                        //Log.i(LOG_TAG, "DOWNLOAD NEAR STATION FAILED");
                        stationTask = (StationTask)msg.obj;
                        stationTask.getStationListView().showStationListView(View.VISIBLE);
                        stationTask.getStationListFragment().setStationList(null);

                        stationTask.recycle();
                        mStationTaskQueue.offer(stationTask);
                        break;

                    case POPULATE_STATION_LIST_COMPLETED:
                        //Log.i(LOG_TAG, "POPULATE_STATION_LIST_COMPLETED");
                        stationTask = (StationTask)msg.obj;

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
                        stationTask = (StationTask)msg.obj;
                        stationTask.recycle();
                        mStationTaskQueue.offer(stationTask);
                        break;

                    case DOWNLOAD_DISTCODE_COMPLTETED:
                        //Log.i(LOG_TAG, "DOWNLOAD_DISTCODE_COMPLETED");
                        distCodeTask = (DistCodeTask)msg.obj;
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

        if(task instanceof DistCodeTask) {
            switch (state) {
                case DOWNLOAD_DISTCODE_COMPLTETED:
                    msg.sendToTarget();
                    break;
                case DOWNLOAD_DISTCODE_FAILED:
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof PriceTask) {
            switch(state) {
                case DOWNLOAD_PRICE_COMPLETE:
                    msg.sendToTarget();
                    break;
                case DOWNLOAD_PRICE_FAILED:
                    break;
            }

        } else if(task instanceof LocationTask) {
            switch (state) {
                case FETCH_LOCATION_COMPLETED: msg.sendToTarget(); break;
                case FETCH_LOCATION_FAILED: msg.sendToTarget(); break;
            }

        } else if(task instanceof StationTask) {

            switch (state) {

                case DOWNLOAD_STATION_LIST_COMPLETE:
                    log.i("DOWNLOAD_STATION_LIST_COMPLETE");
                    /*
                    List<Opinet.GasStnParcelable> stationList = ((StationTask) task).getStationList();
                    for(Opinet.GasStnParcelable station : stationList) {
                        ((StationTask) task).initStationInfo(station);
                        mDownloadThreadPool.execute(((StationTask) task).getStationInfoRunnalbe());
                    }
                    */
                    //mDownloadThreadPool.execute(((StationTask) task).getStationInfoRunnalbe());
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_STATION_INFO_COMPLETE:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NO_STATION_COMPLETE:
                    log.i("DOWNLOAD_NO_STATION_COMPLETE");
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NEAR_STATIONS_FAILED:
                    log.i("DOWNLOAD_NEAR_STATIONS_FAILED");
                    msg.sendToTarget();
                    break;
            }

        } else {
            msg.sendToTarget();
        }

        /*
        } else if(task instanceof PriceTask) {

            switch(state) {
                case DOWNLOAD_PRICE_COMPLETE:
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

        } else if(task instanceof StationTask) {

            switch (state) {

                case DOWNLOAD_STATION_LIST_COMPLETE:
                    mDownloadThreadPool.execute(((StationTask) task).getStationListRunnable());
                    msg.sendToTarget(); //pass the downloaded list back to OpinetStationListFragment
                    break;

                case DOWNLOAD_NO_STATION_COMPLETE:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NEAR_STATIONS_FAILED:
                    msg.sendToTarget();
                    break;

                case POPULATE_STATION_LIST_COMPLETED:
                    msg.sendToTarget();
                    break;

                case POPULATE_STATION_LIST_FAILED:
                    //mDownloadThreadPool.execute(((StationTask) task).getStationListRunnable());
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof StationCurrentTask) {

            switch(state) {
                case DOWNLOAD_CURRENT_STATION_COMPLETED:
                    mDownloadThreadPool.execute(((StationCurrentTask) task).getStationInfoRunnable());
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_NO_STATION_COMPLETE:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_CURRENT_STATION_FAILED:
                    msg.sendToTarget();
                    break;

                case DOWNLOAD_STATION_INFO_COMPLETE:
                    msg.sendToTarget();
                    break;
            }

        } else if(task instanceof DistCodeTask) {

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
    public static DistCodeTask startOpinetDistCodeTask (Context context) {

        DistCodeTask task = (DistCodeTask)sInstance.mDownloadWorkQueue.poll();

        if(task == null) {
            task = new DistCodeTask(context);
        }

        sInstance.mDownloadThreadPool.execute(task.getOpinetDistCodeRunnable());

        return task;
    }

    // Retrieves Sigun list with a sido code given in GeneralSettingActivity
    public static SpinnerDistCodeTask startSpinnerDistCodeTask(SpinnerDialogPreference pref, int code) {

        SpinnerDistCodeTask task = (SpinnerDistCodeTask)sInstance.mDownloadWorkQueue.poll();
        if(task == null) task = new SpinnerDistCodeTask(pref.getContext());

        task.initSpinnerDistCodeTask(ThreadManager.sInstance, pref, code);
        sInstance.mDecodeThreadPool.execute(task.getSpinnerDistCodeRunnable());
        return task;
    }


    // Downloads the average, Sido, and Sigun price from the opinet and saves them in the specified
    // file location.

    public static PriceTask startPriceTask(Activity activity, String distCode) {

        PriceTask priceTask = (PriceTask)sInstance.mDownloadWorkQueue.poll();

        if(priceTask == null) {
            log.i("PriceTask: " + priceTask);
            priceTask = new PriceTask(activity);
        }

        priceTask.initPriceTask(ThreadManager.sInstance, activity, distCode);
        sInstance.mDownloadThreadPool.execute(priceTask.getAvgPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceTask.getSidoPriceRunnable());
        sInstance.mDownloadThreadPool.execute(priceTask.getSigunPriceRunnable());

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
    public static StationTask startStationListTask(
            StationRecyclerView view, String[] params, Location location) {

        StationTask stationTask = sInstance.mStationTaskQueue.poll();
        if(stationTask == null) {
            stationTask = new StationTask();
        }

        // Attach OnCompleteTaskListener
        if(sInstance.mTaskListener == null) {
            try {
                sInstance.mTaskListener = (OnCompleteTaskListener) view;
            } catch (ClassCastException e) {
                throw new ClassCastException(String.valueOf(view) + " must implement OnCompleteTaskListener");
            }
        }

        stationTask.initStationTask(ThreadManager.sInstance, view, params, location);
        sInstance.mDownloadThreadPool.execute(stationTask.getStationListRunnable());

        return stationTask;
    }

    /*
    public static StationInfoTask startStationInfoTask(Opinet.GasStnParcelable station) {

        StationInfoTask stationInfoTask = sInstance.mStationInfoTaskQueue.poll();

        if(stationInfoTask == null) {
            stationInfoTask = new StationInfoTask();
        }

        stationInfoTask.initDownloadInfoTask(ThreadManager.sInstance, station);
        sInstance.mDecodeThreadPool.execute(stationInfoTask.getStationInfoRunnable());

        return stationInfoTask;
    }
    */

    /*
    public static StationTask sortNearStationsTask(
            OpinetStationListFragment fm, StationListView view, List<Opinet.GasStnParcelable> stationList) {

        StationTask stationListTask = sInstance.mStationTaskQueue.poll();

        if(stationListTask == null) {
            stationListTask = new StationTask(view.getContext());
        }

        stationListTask.initSortTask(ThreadManager.sInstance, fm, view, stationList);
        sInstance.mDownloadThreadPool.execute(stationListTask.getStationListRunnable());

        return stationListTask;
    }

    */

    @SuppressWarnings("unchecked")
    public static LocationTask fetchLocationTask(Fragment fm){

        LocationTask locationTask = sInstance.mLocationTaskQueue.poll();

        // Attach OnCompleteTaskListener
        try {
            sInstance.mTaskListener = (OnCompleteTaskListener)fm;
        } catch(ClassCastException e) {
            throw new ClassCastException(fm.toString() + " must implement OnCompleteTaskListener");
        }

        if(locationTask == null) {
            locationTask = new LocationTask(fm.getContext());
        }

        locationTask.initLocationTask(ThreadManager.sInstance);
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
