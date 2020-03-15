package com.silverback.carman2.viewmodels;

import android.text.style.ImageSpan;
import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class FirestoreViewModel extends ViewModel {

    private MutableLiveData<Boolean> autoResourceTaskDone;

    public MutableLiveData<Boolean> getAutoResourceTaskDone() {
        if(autoResourceTaskDone == null) autoResourceTaskDone = new MutableLiveData<>();
        return autoResourceTaskDone;
    }

}
