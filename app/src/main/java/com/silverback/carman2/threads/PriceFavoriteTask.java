package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.OpinetPriceViewModel;

public class PriceFavoriteTask extends ThreadTask implements PriceFavoriteRunnable.StationPriceMethods {

    // Objects
    private OpinetPriceViewModel viewModel;
    private Runnable mPriceRunnableStation;
    private String stnId;

    PriceFavoriteTask(Context context) {
        super();
        mPriceRunnableStation = new PriceFavoriteRunnable(context, this);
    }

    void initTask(OpinetPriceViewModel model, String stnId) {
        viewModel = model;
        this.stnId = stnId;
    }

    Runnable getPriceRunnableStation() {
        return mPriceRunnableStation;
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    @Override
    public void setStnPriceThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void saveStationPriceData() {
        viewModel.notifyPriceComplete().postValue(true);
    }
}
