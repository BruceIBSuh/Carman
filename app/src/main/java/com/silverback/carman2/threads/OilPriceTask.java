package com.silverback.carman2.threads;

import android.content.Context;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.OpinetViewModel;

import java.util.List;

public class OilPriceTask extends ThreadTask implements OilPriceRunnable.OpinetPriceListMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OilPriceTask.class);

    // Objects and Fields
    private OpinetViewModel viewModel;
    private SparseArray<Object> sparseArray;
    private Runnable mAvgPriceRunnable, mSidoPriceRunnable, mSigunPriceRunnable, mStationPriceRunnable;
    private String distCode;
    private String stnId;
    private int index = 0;

    // Constructor: creates an OilPriceTask object containing OilPriceRunnable object.
    OilPriceTask(Context context) {
        super();

        sparseArray = new SparseArray<>();
        mAvgPriceRunnable = new OilPriceRunnable(context, this, OilPriceRunnable.AVG);
        mSidoPriceRunnable = new OilPriceRunnable(context, this, OilPriceRunnable.SIDO);
        mSigunPriceRunnable = new OilPriceRunnable(context, this, OilPriceRunnable.SIGUN);
        mStationPriceRunnable = new OilPriceRunnable(context, this, OilPriceRunnable.STATION);

    }

    // Initialize args for OilPriceRunnable
    void initPriceTask(OpinetViewModel viewModel, String distCode, String stnId) {
    //void initPriceTask(OpinetViewModel viewModel, String distCode) {
        this.viewModel = viewModel;
        this.distCode = distCode;
        this.stnId = stnId;
    }

    // Getter for the Runnable invoked by startOilPriceTask() in ThreadManager
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


    // Callback methods defined in OilPriceRunnable.OpinentPriceListMethods
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
    public synchronized void setOilPrice(int mode, Object obj) {
        index++;

        sparseArray.put(mode, obj);
        // When initiating the app first time, the station id doesn't exist. Thus, no price info
        // of the favorite station shouldn't be provided. Other than this case, the price info should
        // be provided 4 times(avg, sido, sigun, station).
        //if(index >= ((stnId == null)? 3 : 4)) viewModel.distOilPriceComplete().postValue(true);
        if(index == 4) viewModel.getOilPriceData().postValue(sparseArray);
    }

    @Override
    public void handlePriceTaskState(int state) {
        int outstate = -1;

        switch(state) {
            case OilPriceRunnable.DOWNLOAD_PRICE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_PRICE_COMPLETED;
                break;

            case OilPriceRunnable.DOWNLOAD_PRICE_FAILED:
                outstate = ThreadManager.DOWNLOAD_PRICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}
