package com.silverback.carman2.models;

import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FirestoreViewModel extends ViewModel {

    private MutableLiveData<SparseArray> favoriteSnapshot;

    public MutableLiveData<SparseArray> getFavoriteSnapshot() {
        if(favoriteSnapshot == null) favoriteSnapshot = new MutableLiveData<>();
        return favoriteSnapshot;
    }

}
