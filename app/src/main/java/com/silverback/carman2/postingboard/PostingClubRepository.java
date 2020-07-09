package com.silverback.carman2.postingboard;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class PostingClubRepository  {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingClubRepository.class);

    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private ListenerRegistration mListener;

    // Fields
    private String field;
    private boolean isViewOrder;


    // Constructor
    public PostingClubRepository() {
        log.i("PostingClubRepository");
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");
        query = colRef;
    }

    public void queryClubPosting(boolean b) {
        this.field = (isViewOrder)? "cnt_view" : "timestamp";
        query = query.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION);
    }

    public void queryNextClubPosint(QuerySnapshot querySnapshot) {
        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        query = query.orderBy(field, Query.Direction.DESCENDING).startAfter(lastDoc)
                .limit(Constants.PAGINATION);
    }
}
