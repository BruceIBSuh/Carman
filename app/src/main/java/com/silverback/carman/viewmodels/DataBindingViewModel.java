package com.silverback.carman.viewmodels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DataBindingViewModel extends ViewModel {

    private MutableLiveData<String> sidoName;
    private MutableLiveData<String> sigunName;

    private MutableLiveData<String> avgprice;
    private MutableLiveData<String> sidoprice;
    private MutableLiveData<String> sigunprice;

    public MutableLiveData<String> getSidoName() {
        if(sidoName == null) sidoName = new MutableLiveData<>();
        return sidoName;
    }

    public MutableLiveData<String> getSigunName() {
        if(sigunName == null) sigunName = new MutableLiveData<>();
        return sigunName;
    }

    public MutableLiveData<String> getAvgPrice() {
        if(avgprice == null) avgprice = new MutableLiveData<>();
        return avgprice;
    }

    public MutableLiveData<String> getSidoPrice() {
        if(sidoprice == null) sidoprice = new MutableLiveData<>();
        return sidoprice;
    }

    public MutableLiveData<String> getSigunPrice() {
        if(sigunprice == null) sigunprice = new MutableLiveData<>();
        return sigunprice;
    }

}
