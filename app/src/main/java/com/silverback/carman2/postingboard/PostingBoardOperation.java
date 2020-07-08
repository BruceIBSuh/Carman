package com.silverback.carman2.postingboard;

import com.google.firebase.firestore.DocumentSnapshot;

public class PostingBoardOperation {

    private DocumentSnapshot snapshot;
    private int type;

    public PostingBoardOperation(DocumentSnapshot snapshot, int type){
        this.snapshot = snapshot;
        this.type = type;
    }

    public int getType() {
        return type;
    }
    public DocumentSnapshot getSnapshot() {
        return snapshot;
    }
}
