package com.silverback.carman2.models;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<String> selected = new MutableLiveData<>();

    public void setInputValue(String data) {
        selected.setValue(data);
    }
    public LiveData<String> getInputValue() {
        return selected;
    }

}
