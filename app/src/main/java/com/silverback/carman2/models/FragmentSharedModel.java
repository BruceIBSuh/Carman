package com.silverback.carman2.models;

import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();
    private final MutableLiveData<SparseIntArray> selectedValue = new MutableLiveData<>();
    private final MutableLiveData<SparseArray> selectedMemo = new MutableLiveData<>();
    private final MutableLiveData<JSONObject> jsonServiceItemObj = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertGasResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertSvcResult = new MutableLiveData<>();
    private final MutableLiveData<SparseArray> svcLocation = new MutableLiveData<>();

    private MutableLiveData<String> favoriteStnName;
    private MutableLiveData<String> favoriteSvcName;
    private MutableLiveData<String> favoriteStnId;
    private MutableLiveData<Integer> imageItemSelected;


    // Communicate b/w ExpensePagerFragment and a fragment contained in the tab viewpager
    public void setCurrentFragment(Fragment fm) { fragment.setValue(fm); }
    public LiveData<Fragment> getCurrentFragment() { return fragment; }

    public void setSelectedValue(int key, int value) {
        SparseIntArray sparsesArray = new SparseIntArray(1); //param: initial capacity.
        sparsesArray.put(key, value);
        selectedValue.setValue(sparsesArray);
    }

    public LiveData<SparseIntArray> getSelectedValue() {
        return selectedValue;
    }


    public void setSelectedMemo(int key, String value) {
        SparseArray<String> sparseArray = new SparseArray<>(1);
        sparseArray.put(key, value);
        selectedMemo.setValue(sparseArray);
    }
    public LiveData<SparseArray> getSelectedMenu() {
        return selectedMemo;
    }


    // Communicate b/w SettingServiceItemFragment and SettingSvcItemDlgFragment to modify the
    // service item list.
    public void setServiceItem(JSONObject jsonObject) {
        jsonServiceItemObj.setValue(jsonObject);
    }
    public LiveData<JSONObject> getJsonServiceItemObject() {
        return jsonServiceItemObj;
    }

    // Communicate b/w AlertDialogFragment and fragment calling the dialog fragment
    public void setAlert(boolean b) {
        alertResult.setValue(b);
    }
    public LiveData<Boolean> getAlert() {
        return alertResult;
    }

    // Commmunicate b/w RegisterDialogFragment and ServiceManagerFragment
    public void setServiceLocation(SparseArray sparseArray) {
        svcLocation.setValue(sparseArray);
    }
    public LiveData<SparseArray> getServiceLocation() {
        return svcLocation;
    }


    public MutableLiveData<String> getFavoriteStnName() {
        if(favoriteStnName == null) favoriteStnName = new MutableLiveData<>();
        return favoriteStnName;
    }

    public MutableLiveData<String> getFavoriteSvcName() {
        if(favoriteSvcName == null) favoriteSvcName = new MutableLiveData<>();
        return favoriteSvcName;
    }

    public MutableLiveData<String> getFavoriteStnId() {
        if(favoriteStnId == null) favoriteStnId = new MutableLiveData<>();
        return favoriteStnId;
    }

    // Communicate GasManagerFragment or ServiceManagerFragment w/ AlertDidalogFragment when
    // the favorite button clicks to remove a provider out of the favorite list.
    public void setAlertGasResult(boolean b) {
        alertGasResult.setValue(b);
    }
    public MutableLiveData<Boolean> getAlertGasResult() {
        return alertGasResult;
    }


    public void setAlertSvcResult(boolean b) {
        alertSvcResult.setValue(b);
    }

    public MutableLiveData<Boolean> getAlertSvcResult() {
        return alertSvcResult;
    }

    public MutableLiveData<Integer> getImageItemSelected() {
        if(imageItemSelected == null) imageItemSelected = new MutableLiveData<>();
        return imageItemSelected;
    }


}
