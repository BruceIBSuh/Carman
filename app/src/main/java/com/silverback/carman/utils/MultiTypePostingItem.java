package com.silverback.carman.utils;

import com.google.firebase.firestore.DocumentSnapshot;

public class MultiTypePostingItem {
    private DocumentSnapshot snapshot;
    private final int viewType;

    // constructor for the type of DocumentSnapshot
    public MultiTypePostingItem(int viewType, DocumentSnapshot snapshot) {
        this.snapshot = snapshot;
        this.viewType = viewType;
    }

    // constructor for the type of AD
    public MultiTypePostingItem(int viewType){
        this.viewType = viewType;
    }
    public DocumentSnapshot getSnapshot() {
        return snapshot;
    }
    public int getViewType() {
        return viewType;
    }

    public int getViewId() {
        return this.hashCode();
    }


}
