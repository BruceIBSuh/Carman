package com.silverback.carman2.models;

import android.content.ClipData;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<String> selected = new MutableLiveData<>();
    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();

    public void setInputValue(String data) {
        selected.setValue(data);
    }
    public LiveData<String> getInputValue() {
        return selected;
    }

    // Communicate b/w RecentExpPagerFragment and a fragment contained in the bottom viewpagere
    public void setCurrentFragment(Fragment fm) { fragment.setValue(fm); }
    public LiveData<Fragment> getCurrentFragment() { return fragment; }

}
