package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.Map;

public class StationFavTask extends ThreadTask implements StationFavRunnable.StationPriceMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationFavTask.class);

    // Objects
    private StationListViewModel viewModel;
    //private WeakReference<OpinetViewModel> weakModelReference;
    private final Runnable mFavStationRunnable;
    private String stnId;
    private boolean isFirst;

    StationFavTask(Context context) {
        super();
        mFavStationRunnable = new StationFavRunnable(context, this);
    }

    void initTask(StationListViewModel model, String stnId, boolean isFirst) {
        this.viewModel = model;
        //weakModelReference = new WeakReference<>(model);
        this.stnId = stnId;
        this.isFirst = isFirst;//check whether it is the firstholder or a station in the list.
    }

    Runnable getPriceRunnableStation() {
        return mFavStationRunnable;
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
        //weakModelReference.get().getFavoritePriceData().postValue(data);
    }

    @Override
    public void setFavStationInfo(StationFavRunnable.Info info) {
        viewModel.getFavStationInfo().postValue(info);
    }


    @Override
    public void savePriceDiff() {
        //weakModelReference.get().favoritePriceComplete().postValue(true);
    }

    public void recycle() {

    }
}
