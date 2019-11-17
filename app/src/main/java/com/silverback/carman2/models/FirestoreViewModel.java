package com.silverback.carman2.models;

import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;

public class FirestoreViewModel extends ViewModel {

    private MutableLiveData<SparseArray> favoriteSnapshot;
    private MutableLiveData<DocumentSnapshot> postSnapshot;
    private MutableLiveData<DocumentSnapshot> userSnapshot;

    public MutableLiveData<SparseArray> getFavoriteSnapshot() {
        if(favoriteSnapshot == null) favoriteSnapshot = new MutableLiveData<>();
        return favoriteSnapshot;
    }

    public MutableLiveData<DocumentSnapshot> getPostSnapshot() {
        if(postSnapshot == null) postSnapshot = new MutableLiveData<>();
        return postSnapshot;
    }

    public MutableLiveData<DocumentSnapshot> getUserSnapshot() {
        if(userSnapshot == null) userSnapshot = new MutableLiveData();
        return userSnapshot;
    }

}
