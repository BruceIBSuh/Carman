package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;

public class GasPriceTask extends ThreadTask implements GasPriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasPriceTask.class);

    // Objects and Fields
    private OpinetViewModel viewModel;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    // Constructor: creates an GasPriceTask object containing GasPriceRunnable object.
    GasPriceTask(Context context) {
        super();
        mAvgPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.AVG);
        mSidoPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIDO);
        mSigunPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIGUN);
        mStationPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.STATION);

    }

    // Initialize args for GasPriceRunnable
    void initPriceTask(OpinetViewModel viewModel, String distCode, String stnId) {
        this.viewModel = viewModel;
        this.distCode = distCode;
        this.stnId = stnId;
    }

    // Getter for the Runnable invoked by startGasPriceTask() in ThreadManager
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


    // Callback methods defined in GasPriceRunnable.OpinentPriceListMethods
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
    public void addPriceCount() {
        index++;
        if(index == 4) viewModel.distPriceComplete().postValue(true);
    }

    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;

        switch(state) {
            case GasPriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETED;
                break;

            case GasPriceRunnable.DOWNLOAD_PRICE_FAILED:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

    public void recycle(){
        index = 0;
    }

}
