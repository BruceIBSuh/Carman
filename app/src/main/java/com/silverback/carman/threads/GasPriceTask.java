package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.lang.ref.WeakReference;

/**
 * Ths class is to retrieve the gas prices respectively by average, sido, sigun and the first
 * favorite gas station.
 */

public class GasPriceTask extends ThreadTask implements GasPriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasPriceTask.class);

    // Objects and Fields
    //private OpinetViewModel viewModel;
    private WeakReference<OpinetViewModel> weakModelReference;
    private final Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable;//, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    GasPriceTask(Context context) {
        super();
        mAvgPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.AVG);
        mSidoPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIDO);
        mSigunPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.SIGUN);
        //mStationPriceRunnable = new GasPriceRunnable(context, this, GasPriceRunnable.STATION);
    }

    // Initialize args
    void initPriceTask(OpinetViewModel viewModel, String distCode) {
        //this.viewModel = viewModel;
        this.distCode = distCode;
        this.weakModelReference = new WeakReference<>(viewModel);
        //this.stnId = stnId;
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
    //Runnable getStationPriceRunnable() { return mStationPriceRunnable; }


    // Callback methods defined in GasPriceRunnable.OpinentPriceListMethods
    @Override
    public void setGasPriceThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public String getDistrictCode() {
        return distCode;
    }

    /*
    @Override
    public String getStationId() {
        return stnId;
    }
     */

    // Separate the gas price by category and handle it with corresponding viewmodel
    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;
        index ++;
        if(index == 3) {
            log.i("gas task done");
            weakModelReference.get().distPriceComplete().postValue(true);
            switch (state) {
                case GasPriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                    outstate = ThreadManager2.TASK_COMPLETE;
                    break;
                case GasPriceRunnable.DOWNLOAD_PRICE_FAILED:
                    outstate = ThreadManager2.TASK_FAIL;
                    break;
            }

            sThreadManager.handleState(this, outstate);
        }
    }

    public void recycle(){
        index = 0;
        if(weakModelReference != null) {
            weakModelReference.clear();
            weakModelReference = null;
        }
    }
}
