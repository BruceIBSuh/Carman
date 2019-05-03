package com.silverback.carman2.models;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<String> selected = new MutableLiveData<>();

    public void setInputValue(String value) {
        selected.setValue(value);
    }

    public LiveData<String> getInputValue() {
        return selected;
    }

}
