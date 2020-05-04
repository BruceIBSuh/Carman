package com.silverback.carman2.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

/**
 * This viewmodel is designed to store and maange UI-related data from Opinet in a lifecycle-concious
 * way. The data will survive confiuguration changs such as screen rotations.
 */
public class OpinetViewModel extends ViewModel {

    private MutableLiveData<Boolean> distCode;
    private MutableLiveData<Boolean> districtPrice;
    private MutableLiveData<Boolean> favoritePrice;
    private MutableLiveData<Map<String, Float>> favoritePriceData;


    public MutableLiveData<Boolean> distCodeComplete() {
        if(distCode == null) distCode = new MutableLiveData<>();
        return distCode;
    }

    public MutableLiveData<Boolean> distPriceComplete() {
        if(districtPrice == null) districtPrice = new MutableLiveData<>();
        return districtPrice;
    }

    //
    public MutableLiveData<Boolean> favoritePriceComplete() {
        if(favoritePrice == null) favoritePrice = new MutableLiveData<>();
        return favoritePrice;
    }

    public MutableLiveData<Map<String, Float>> getFavoritePriceData() {
        if(favoritePriceData == null) favoritePriceData = new MutableLiveData<>();
        return favoritePriceData;
    }
}
