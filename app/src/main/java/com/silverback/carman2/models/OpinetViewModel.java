package com.silverback.carman2.models;

import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;

public class OpinetViewModel extends ViewModel {

    private MutableLiveData<SparseArray<Object>> oilPriceData;
    private MutableLiveData<Boolean> districtPrice;
    private MutableLiveData<Boolean> distCode;
    private MutableLiveData<Boolean> favoritePrice;
    private MutableLiveData<Map<String, Float>> favoritePriceData;


    public MutableLiveData<Boolean> distCodeComplete() {
        if(distCode == null) distCode = new MutableLiveData<>();
        return distCode;
    }

    public MutableLiveData<Boolean> getDistPriceComplete() {
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
