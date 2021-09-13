package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

/**
 * Ths class is to retrieve the gas prices respectively by average, sido, sigun and the first
 * favorite gas station.
 */

public class GasPriceTask extends ThreadTask implements GasPriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasPriceTask.class);

    // Objects and Fields
    private OpinetViewModel viewModel;
    private final Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    GasPriceTask(Context context) {
        super();
        mAvgPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.AVG);
        mSidoPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIDO);
        mSigunPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIGUN);
        mStationPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.STATION);
    }

    // Initialize args
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
    public void setGasPriceThread(Thread currentThread) {
        setCurrentLocation(currentThread);
    }

    @Override
    public String getDistrictCode() {
        return distCode;
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    // Separate the gas price by category and handle it with corresponding viewmodel
    @Override
    public void handlePriceTaskState(int state) {
        index ++;
        int outstate = -1;
        state *= state;
        log.i("price index and state: %s, %s", index, state);

        // If all price data of average, sido, sigun and favorite station are retrieved, notify
        // the viewmodel of the task done and finalize the task in the main thread.
        if(index == 4) {
            viewModel.distPriceComplete().postValue(true);
            switch (state) {
                case GasPriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                    outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETED;
                    break;

                case GasPriceRunnable.DOWNLOAD_PRICE_FAILED:
                    outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                    break;
            }

            sThreadManager.handleState(this, outstate);
        }
    }

    @Override
    protected void recycle(){
        stnId = null;
        distCode = null;
        index = 0;
    }

}
