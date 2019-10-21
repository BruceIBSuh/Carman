package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;

public class PriceDistrictTask extends ThreadTask implements PriceDistrictRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(PriceDistrictTask.class);

    // Objects and Fields
    private OpinetViewModel viewModel;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    // Constructor: creates an PriceDistrictTask object containing PriceDistrictRunnable object.
    PriceDistrictTask(Context context) {
        super();
        mAvgPriceRunnable = new PriceDistrictRunnable(context, this, PriceDistrictRunnable.AVG);
        mSidoPriceRunnable = new PriceDistrictRunnable(context, this, PriceDistrictRunnable.SIDO);
        mSigunPriceRunnable = new PriceDistrictRunnable(context, this, PriceDistrictRunnable.SIGUN);
        mStationPriceRunnable = new PriceDistrictRunnable(context, this, PriceDistrictRunnable.STATION);

    }

    // Initialize args for PriceDistrictRunnable
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


    // Callback methods defined in PriceDistrictRunnable.OpinentPriceListMethods
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
        // When initiating the app first time, the station id doesn't exist. Thus, no price info
        // of the favorite station shouldn't be provided. Other than this case, the price info should
        // be provided 4 times(avg, sido, sigun, station).
        if(index >= ((stnId == null)? 3 : 4)) viewModel.notifyPriceComplete().postValue(true);
    }

    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;

        switch(state) {
            case PriceDistrictRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETED;
                break;

            case PriceDistrictRunnable.DOWNLOAD_PRICE_FAILED:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}
