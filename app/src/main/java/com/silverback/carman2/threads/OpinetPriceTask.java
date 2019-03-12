package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;

import java.lang.ref.WeakReference;

public class OpinetPriceTask extends ThreadTask implements OpinetPriceRunnable.OpinetPriceListMethods {

    // Constants
    private static final String TAG = "OpinetPriceTask";

    // Objects and Fields
    private WeakReference<Activity> mWeakActivity;
    private Runnable mOpinetPriceListRunnable;
    private String distCode;
    private int distSort;
    //private boolean isAvgDownloaded, isSidoDownloaded, isSigunDownloaded;

    // Constructor: creates an OpinetPriceTask object containing OpinetPriceRunnable object.
    OpinetPriceTask(Context context) {
        super();
        mOpinetPriceListRunnable = new OpinetPriceRunnable(context, this);
    }

    // Initialize args for OpinetPriceRunnable
    void initOpinetPriceTask(ThreadManager threadManager, Activity activity, String distCode, int distSort) {
        sThreadManager = threadManager;
        mWeakActivity = new WeakReference<>(activity);
        this.distCode = distCode;
        this.distSort = distSort;
    }

    // Getter for the Runnable invoked by startOpinetPriceTask() in ThreadManager
    Runnable getOpinetPriceListRunnable() {return mOpinetPriceListRunnable;}


    // Callback methods defined in OpinetPriceRunnable.OpinentPriceListMethods
    @Override
    public void setPriceDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public String getDistrictCode() {
        return distCode;
    }

    @Override
    public int getDistrictSort() {
        return distSort;
    }

    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;

        switch(state) {
            case OpinetPriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETED;
                break;
            case OpinetPriceRunnable.DOWNLOAD_PRICE_FAIL:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

    public Activity getParentActivity(){
        if(mWeakActivity != null) return mWeakActivity.get();
        return null;
    }
}
