package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.lang.ref.WeakReference;
import java.util.Map;

public class FavStationTaskk extends ThreadTask implements FavStationRunnable.StationPriceMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavStationTaskk.class);

    // Objects
    private OpinetViewModel viewModel;
    private WeakReference<OpinetViewModel> weakModelReference;
    private final Runnable mPriceRunnableStation;
    private String stnId;
    private boolean isFirst;

    FavStationTaskk(Context context) {
        super();
        mPriceRunnableStation = new FavStationRunnable(context, this);
    }

    void initTask(OpinetViewModel model, String stnId, boolean isFirst) {
        //viewModel = model;
        weakModelReference = new WeakReference<>(model);
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
        //viewModel.getFavoritePriceData().postValue(data);
        weakModelReference.get().getFavoritePriceData().postValue(data);
    }

    @Override
    public void savePriceDiff() {
        weakModelReference.get().favoritePriceComplete().postValue(true);
    }

    public void recycle() {
        if(weakModelReference != null) {
            weakModelReference.clear();
            weakModelReference = null;
        }
    }
}
