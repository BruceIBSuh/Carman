package com.silverback.carman2.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class BoardViewModel extends ViewModel {

    private MutableLiveData<Boolean> generalPost;
    private MutableLiveData<ArrayList<CharSequence>> autofilterValues;

    public MutableLiveData<ArrayList<CharSequence>> getAutoFilterValues() {
        if(autofilterValues == null) autofilterValues = new MutableLiveData<>();
        return autofilterValues;
    }

    public MutableLiveData<Boolean> getGeneralPost() {
        if(generalPost == null) generalPost = new MutableLiveData<>();
        return generalPost;
    }

}
