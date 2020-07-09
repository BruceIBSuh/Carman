package com.silverback.carman2.postingboard;

import com.google.firebase.firestore.DocumentSnapshot;

class ClubBoardOperation {

    private DocumentSnapshot snapshot;
    private int type;

    public ClubBoardOperation(DocumentSnapshot snapshot, int type){
        this.snapshot = snapshot;
        this.type = type;
    }

    public int getType() {
        return type;
    }
    public DocumentSnapshot getDocumentSnapshot() {
        return snapshot;
    }
}
