package com.silverback.carman2.threads;

import android.util.SparseArray;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.models.FirestoreViewModel;

import java.util.List;

public class FirestoreTask extends ThreadTask implements FirestoreRunnable.FirestoreMethods {

    // Objects
    private List<FavoriteProviderEntity> favoriteList;
    private FirestoreRunnable mFirestoreRunnable;
    private FirestoreViewModel viewModel;
    private SparseArray<DocumentSnapshot> sparseArray;
    private int category;

    // Constructor
    FirestoreTask() {
        super();
        mFirestoreRunnable = new FirestoreRunnable(this);
        sparseArray = new SparseArray<>();
    }

    void initFirestoreTask(
            List<FavoriteProviderEntity> favoriteList, FirestoreViewModel viewModel, int category) {

        this.favoriteList = favoriteList;
        this.viewModel = viewModel;
        this.category = category;
    }

    Runnable getFirestoreRunnable() {
        return mFirestoreRunnable;
    }

    @Override
    public List<FavoriteProviderEntity> getFavoriteList() {
        return favoriteList;
    }

    @Override
    public void setFirestoreThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setFavoriteSnapshot(int position, DocumentSnapshot snapshot) {
        sparseArray.put(position, snapshot);
        if(sparseArray.size() == favoriteList.size()) {
            viewModel.getFavoriteSnapshot().postValue(sparseArray);
        }
    }

    @Override
    public void handleFavoriteState(int state) {

    }




}
