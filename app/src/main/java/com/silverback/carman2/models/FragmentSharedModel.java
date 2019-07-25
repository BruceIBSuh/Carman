package com.silverback.carman2.models;

import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();
    private final MutableLiveData<SparseIntArray> selectedValue = new MutableLiveData<>();
    private final MutableLiveData<SparseArray> selectedMemo = new MutableLiveData<>();
    private final MutableLiveData<JSONObject> jsonServiceItemObj = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alert = new MutableLiveData<>();

    // Communicate b/w ExpensePagerFragment and a fragment contained in the tab viewpager
    public void setCurrentFragment(Fragment fm) { fragment.setValue(fm); }
    public LiveData<Fragment> getCurrentFragment() { return fragment; }

    /*
     * Share data b/w fragments
     * setSelectedValue(): defined in NumberPadFragment
     * getSelectedValue(): defined in GasManagerFragment or ServiceManagerFragment
     *
     * Pass the values put in NumberPadFragment to GasManagerFragment or ServiceManagerFragment
     * as the type of SparseIntArray, the key of which indicates the id of views in NumberPadFragment.
     */
    public void setSelectedValue(int key, int value) {
        SparseIntArray sparsesArray = new SparseIntArray(1); //param: initial capacity.
        sparsesArray.put(key, value);
        selectedValue.setValue(sparsesArray);
    }

    public LiveData<SparseIntArray> getSelectedValue() {
        return selectedValue;
    }

    /**
     * Share data b/w Input
     * @param key
     * @param value
     */
    public void setSelectedMemo(int key, String value) {
        SparseArray<String> sparseArray = new SparseArray<>(1);
        sparseArray.put(key, value);
        selectedMemo.setValue(sparseArray);
    }
    public LiveData<SparseArray> getSelectedMenu() {
        return selectedMemo;
    }


    // Communicate b/w SettingServiceItemFragment and SettingServiceDlgFragment to modify the
    // service item list.
    public void setServiceItem(JSONObject jsonObject) {
        jsonServiceItemObj.setValue(jsonObject);

    }
    public LiveData<JSONObject> getJsonServiceItemObject() {
        return jsonServiceItemObj;
    }

    // Communicate b/w AlertDialogFragment and fragment calling the dialog fragment
    public void setAlert(boolean b) {
        alert.setValue(b);
    }
    public LiveData<Boolean> getAlert() {
        return alert;
    }

}
