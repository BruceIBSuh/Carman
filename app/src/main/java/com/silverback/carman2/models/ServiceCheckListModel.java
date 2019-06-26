package com.silverback.carman2.models;

import android.util.SparseArray;
import android.util.SparseBooleanArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ServiceCheckListModel extends ViewModel {

    private MutableLiveData<SparseBooleanArray> chkboxState;
    private MutableLiveData<SparseArray<String>> itemCost;
    private MutableLiveData<SparseArray<String>> itemMemo;

    public MutableLiveData<SparseBooleanArray> getChkboxState() {
        if(chkboxState == null) chkboxState = new MutableLiveData<>();
        return chkboxState;
    }

    public MutableLiveData<SparseArray<String>> getItemCost() {
        if(itemCost == null) itemCost = new MutableLiveData<>();
        return itemCost;
    }

    public MutableLiveData<SparseArray<String>> getItemMemo() {
        if(itemMemo == null) itemMemo = new MutableLiveData<>();
        return itemMemo;
    }


}
