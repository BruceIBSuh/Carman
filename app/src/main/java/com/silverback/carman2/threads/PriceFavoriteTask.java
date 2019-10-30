package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.models.OpinetViewModel;

import java.util.Map;

public class PriceFavoriteTask extends ThreadTask implements PriceFavoriteRunnable.StationPriceMethods {

    // Objects
    private OpinetViewModel viewModel;
    private Runnable mPriceRunnableStation;
    private String stnId;
    private boolean isFirst;

    PriceFavoriteTask(Context context) {
        super();
        mPriceRunnableStation = new PriceFavoriteRunnable(context, this);
    }

    void initTask(OpinetViewModel model, String stnId, boolean isFirst) {
        viewModel = model;
        this.stnId = stnId;
        this.isFirst = isFirst;
    }

    Runnable getPriceRunnableStation() {
        return mPriceRunnableStation;
    }

    @Override
    public String getStationId() {
        return stnId;
    }

    @Override
    public boolean getIsFirst() {
        return isFirst;
    }

    @Override
    public void setStnPriceThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setFavoritePrice(Map<String, Float> data) {
        viewModel.getFavoritePriceData().postValue(data);
    }

    @Override
    public void saveStationPriceData() {
        viewModel.favoritePriceComplete().postValue(true);
    }
}
