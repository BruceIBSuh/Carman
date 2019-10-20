package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OpinetViewModel extends ViewModel {

    private MutableLiveData<Boolean> favoritePrice;
    private MutableLiveData<Boolean> distCode;

    public MutableLiveData<Boolean> notifyDistCodeComplete() {
        if(distCode == null) distCode = new MutableLiveData<>();
        return distCode;
    }

    public MutableLiveData<Boolean> notifyPriceComplete() {
        if(favoritePrice == null) favoritePrice = new MutableLiveData<>();
        return favoritePrice;
    }
}
