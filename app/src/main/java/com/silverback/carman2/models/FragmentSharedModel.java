package com.silverback.carman2.models;

import android.content.ClipData;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<String> gasSelected = new MutableLiveData<>();
    private final MutableLiveData<String> svcseltected = new MutableLiveData<>();
    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();

    // Communicate b/w GasManagerFragment or ServiceManagerFragment and InputPadFragment.
    public void setServiceValue(String data) {
        svcseltected.setValue(data);
    }

    public void setGasValue(String data) {
        gasSelected.setValue(data);
    }
    public LiveData<String> getServiceValue() {
        return svcseltected;
    }

    public LiveData<String> getGasValue() {
        return gasSelected;
    }


    // Communicate b/w ExpensePagerFragment and a fragment contained in the bottom viewpager
    public void setCurrentFragment(Fragment fm) { fragment.setValue(fm); }
    public LiveData<Fragment> getCurrentFragment() { return fragment; }

}
