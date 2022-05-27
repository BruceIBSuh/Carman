package com.silverback.carman.utils;

import com.google.firebase.firestore.DocumentSnapshot;

public class MultiTypePostingItem {
    private DocumentSnapshot snapshot;
    private final int viewType;
    private int itemIndex;

    // constructor for the type of DocumentSnapshot
    public MultiTypePostingItem(DocumentSnapshot snapshot, int viewType, int index) {
        this.snapshot = snapshot;
        this.viewType = viewType;
        this.itemIndex = index;
    }
    // constructor for the type of AD
    public MultiTypePostingItem(int viewType){
        this.viewType = viewType;
    }

    public void setItemIndex(int index) {
        this.itemIndex = index;
    }

    public DocumentSnapshot getDocument() {
        return snapshot;
    }
    public int getViewType() {
        return viewType;
    }
    public int getViewId() {
        return this.hashCode();
    }
    public int getItemIndex() { return this.itemIndex; }

}
