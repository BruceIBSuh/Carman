package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;

public class PriceRegionalTask extends ThreadTask implements PriceRegionalRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(PriceRegionalTask.class);

    // Objects and Fields
    private OpinetViewModel viewModel;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    // Constructor: creates an PriceRegionalTask object containing PriceRegionalRunnable object.
    PriceRegionalTask(Context context) {
        super();
        mAvgPriceRunnable = new PriceRegionalRunnable(context, this, PriceRegionalRunnable.AVG);
        mSidoPriceRunnable = new PriceRegionalRunnable(context, this, PriceRegionalRunnable.SIDO);
        mSigunPriceRunnable = new PriceRegionalRunnable(context, this, PriceRegionalRunnable.SIGUN);
        mStationPriceRunnable = new PriceRegionalRunnable(context, this, PriceRegionalRunnable.STATION);

    }

    // Initialize args for PriceRegionalRunnable
    void initPriceTask(OpinetViewModel viewModel, String distCode, String stnId) {
    //void initPriceTask(OpinetViewModel viewModel, String distCode) {
        this.viewModel = viewModel;
        this.distCode = distCode;
        this.stnId = stnId;
    }

    // Getter for the Runnable invoked by startRegionalPriceTask() in ThreadManager
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


    // Callback methods defined in PriceRegionalRunnable.OpinentPriceListMethods
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
            case PriceRegionalRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETE;
                break;

            case PriceRegionalRunnable.DOWNLOAD_PRICE_FAILED:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}
