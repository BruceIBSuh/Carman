package com.silverback.carman2.models;

import android.content.ClipData;
import android.util.SparseArray;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();
    private final MutableLiveData<SparseArray> selectedValue = new MutableLiveData<>();

    /**
     * LiveData<T> not sure which is better b/w HashMap or SparseArray.
     * @param key view id of the view clicked in GasManagerFragment or ServiceManagerFragment
     * @param value input with the number buttons
     */
    public void setSelectedValue(Integer key, String value) {
        SparseArray<String> sparsesArray = new SparseArray<>(1); //param: initial capacity.
        sparsesArray.append(key, value);
        selectedValue.setValue(sparsesArray);
    }
    public LiveData<SparseArray> getSelectedValue() {
        return selectedValue;
    }


    // Communicate b/w ExpensePagerFragment and a fragment contained in the bottom viewpager
    public void setCurrentFragment(Fragment fm) { fragment.setValue(fm); }
    public LiveData<Fragment> getCurrentFragment() { return fragment; }

}
