package com.silverback.carman2.models;

import android.graphics.Bitmap;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class FirestoreViewModel extends ViewModel {

    private MutableLiveData<SparseArray> favoriteSnapshot;
    private MutableLiveData<DocumentSnapshot> postSnapshot;
    private MutableLiveData<DocumentSnapshot> userSnapshot;
    private MutableLiveData<Boolean> hasUploadPosted;
    private MutableLiveData<Boolean> resTaskDone;

    private MutableLiveData<SparseArray<ImageSpan>> attachedImageSpan;

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


    public MutableLiveData<Boolean> getUploadPOst() {
        if(hasUploadPosted == null) hasUploadPosted = new MutableLiveData<>();
        return hasUploadPosted;
    }

    public MutableLiveData<SparseArray<ImageSpan>> getAttachedImageSpanList() {
        if(attachedImageSpan == null) attachedImageSpan = new MutableLiveData<>();
        return attachedImageSpan;
    }

    public MutableLiveData<Boolean> getResTaskDone() {
        if(resTaskDone == null) resTaskDone = new MutableLiveData<>();
        return resTaskDone;
    }
}
