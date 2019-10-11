package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OpinetPriceViewModel extends ViewModel {

    private MutableLiveData<Boolean> favoritePrice;

    public MutableLiveData<Boolean> notifyPriceComplete() {
        if(favoritePrice == null) favoritePrice = new MutableLiveData<>();
        return favoritePrice;
    }
}
