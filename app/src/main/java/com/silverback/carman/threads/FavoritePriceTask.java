package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.util.Map;

public class FavoritePriceTask extends ThreadTask implements FavoritePriceRunnable.StationPriceMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoritePriceTask.class);

    // Objects
    private OpinetViewModel viewModel;
    private final Runnable mPriceRunnableStation;
    private String stnId;
    private boolean isFirst;

    FavoritePriceTask(Context context) {
        super();
        mPriceRunnableStation = new FavoritePriceRunnable(context, this);
    }

    void initTask(OpinetViewModel model, String stnId, boolean isFirst) {
        viewModel = model;
        this.stnId = stnId;
        this.isFirst = isFirst;//check whether it is the firstholder or a station in the list.
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

    // OpinetViewModel LiveData having price data of a favroite gas station in the favorite list
    // which pops up by clicking the fav button in ExpenseGasFragment
    @Override
    public void setFavoritePrice(Map<String, Float> data) {
        viewModel.getFavoritePriceData().postValue(data);
    }

    @Override
    public void savePriceDiff() {
        if(viewModel != null) {
            log.i("savePriceDiff");
            viewModel.favoritePriceComplete().postValue(true);
        }
    }

    public void recycle() {
        isFirst = false;
        stnId = null;
    }
}
