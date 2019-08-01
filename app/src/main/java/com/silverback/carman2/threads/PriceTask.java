package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class PriceTask extends ThreadTask implements PriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(PriceTask.class);

    // Objects and Fields
    private WeakReference<Activity> mWeakActivity;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable;
    private String distCode;
    private int index = 0;

    // Constructor: creates an PriceTask object containing PriceRunnable object.
    PriceTask(Context context) {
        super();
        mAvgPriceRunnable = new PriceRunnable(context, this, PriceRunnable.AVG);
        mSidoPriceRunnable = new PriceRunnable(context, this, PriceRunnable.SIDO);
        mSigunPriceRunnable = new PriceRunnable(context, this, PriceRunnable.SIGUN);
    }

    // Initialize args for PriceRunnable
    void initPriceTask(ThreadManager threadManager, Activity activity, String distCode) {
        sThreadManager = threadManager;
        mWeakActivity = new WeakReference<>(activity);
        this.distCode = distCode;
    }

    // Getter for the Runnable invoked by startPriceTask() in ThreadManager
    Runnable getAvgPriceRunnable() {
        return mAvgPriceRunnable;
    }
    Runnable getSidoPriceRunnable() {
        return mSidoPriceRunnable;
    }
    Runnable getSigunPriceRunnable(){
        return mSigunPriceRunnable;
    }


    // Callback methods defined in PriceRunnable.OpinentPriceListMethods
    @Override
    public void setPriceDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public String getDistrictCode() {
        return distCode;
    }

    // Check if the 3 Runnables successfully complte.
    @Override
    public int getCount() {
        return index;
    }

    @Override
    public void addCount() {
        index++;
    }

    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;

        switch(state) {
            case PriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETE;
                break;

            case PriceRunnable.DOWNLOAD_PRICE_FAILED:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

    Activity getParentActivity(){
        if(mWeakActivity != null) return mWeakActivity.get();
        return null;
    }
}
