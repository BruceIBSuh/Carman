package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetPriceViewModel;

import java.lang.ref.WeakReference;

public class PriceTask extends ThreadTask implements PriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(PriceTask.class);

    // Objects and Fields
    private WeakReference<Activity> mWeakActivity;
    private OpinetPriceViewModel viewModel;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    // Constructor: creates an PriceTask object containing PriceRunnable object.
    PriceTask(Context context) {
        super();
        mAvgPriceRunnable = new PriceRunnable(context, this, PriceRunnable.AVG);
        mSidoPriceRunnable = new PriceRunnable(context, this, PriceRunnable.SIDO);
        mSigunPriceRunnable = new PriceRunnable(context, this, PriceRunnable.SIGUN);
        mStationPriceRunnable = new PriceRunnable(context, this, PriceRunnable.STATION);

    }

    // Initialize args for PriceRunnable
    void initPriceTask(OpinetPriceViewModel viewModel, String distCode, String stnId) {
        this.viewModel = viewModel;
        this.distCode = distCode;
        this.stnId = stnId;
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
    Runnable getStationPriceRunnable() { return mStationPriceRunnable; }


    // Callback methods defined in PriceRunnable.OpinentPriceListMethods
    @Override
    public void setPriceDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public String getDistrictCode() {
        return distCode;
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    // Check if the 3 Runnables successfully complte.
    @Override
    public int getTaskCount() {
        return index;
    }

    @Override
    public synchronized void setTaskCount() {
        index++;
        log.i("Task count: %s", index);
        if(index >= 4) viewModel.notifyPriceComplete().postValue(true);
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

}
