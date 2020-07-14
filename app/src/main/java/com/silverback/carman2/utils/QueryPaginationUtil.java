package com.silverback.carman2.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class QueryPaginationUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryPaginationUtil.class);

    // Objects
    private OnQueryPaginationCallback mCallback;
    private FirebaseFirestore firestore;

    private CollectionReference colRef;
    private Query query;
    private QuerySnapshot querySnapshot;

    // Fields
    private String field;
    private int page;
    private boolean isUpdated;

    // Interface
    public interface OnQueryPaginationCallback {
        void getFirstQueryResult(QuerySnapshot postShots);
        void getNextQueryResult(QuerySnapshot nextShots);
    }

    // Constructor
    public QueryPaginationUtil(FirebaseFirestore firestore, OnQueryPaginationCallback callback) {
        this.firestore = firestore;
        colRef = firestore.collection("board_general");
        mCallback = callback;
    }

    public void setPostQuery(int page, boolean isViewOrder) {
        this.page = page;
        query = colRef;
        querySnapshot = null;

        switch(page){
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                query = query.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";
                query = query.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = query.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            boolean hasPendingChange = querySnapshot.getMetadata().hasPendingWrites();
            log.i("hasPendingChange: %s, %s", page, hasPendingChange);
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);

        });
    }

    public void setNextQuery() {
        DocumentSnapshot lastVisibleShot = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        //log.i("last visible shot: %s", lastVisibleShot);

        query = colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastVisibleShot);
        query.limit(Constants.PAGINATION).addSnapshotListener(MetadataChanges.INCLUDE, (nextSnapshot, e) -> {
            if(e != null) return;

            this.querySnapshot = nextSnapshot;
            mCallback.getNextQueryResult(querySnapshot);
        });
    }
}
