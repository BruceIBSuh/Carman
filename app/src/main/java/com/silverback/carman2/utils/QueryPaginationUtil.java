package com.silverback.carman2.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class QueryPaginationUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryPaginationUtil.class);

    // Objects
    private OnQueryPaginationCallback mCallback;
    private FirebaseFirestore firestore;

    private CollectionReference colRef;
    private Query query;
    private QuerySnapshot querySnapshot;

    // Fields
    private boolean isUpdated;
    private String field;


    // Interface
    public interface OnQueryPaginationCallback {
        void getFirstQueryResult(QuerySnapshot postShots);
        void getNextQueryResult(QuerySnapshot nextShots);
        void getLastQueryResult(QuerySnapshot lastShots);
    }

    // Constructor
    public QueryPaginationUtil(FirebaseFirestore firestore, OnQueryPaginationCallback callback) {
        this.firestore = firestore;
        colRef = firestore.collection("board_general");
        mCallback = callback;
        //firestore.clearPersistence();
    }

    public void setPostQuery(int page, boolean isViewOrder) {
        query = colRef;
        querySnapshot = null;

        switch(page){
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        });

    }

    public void setCommentQuery(DocumentReference docRef){
        querySnapshot = null;
        query = docRef.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(Constants.PAGINATION);

        query.addSnapshotListener((commentSnapshot, e) -> {
            // exceepthion handling required
            if(e != null || commentSnapshot == null) return;

            // What if the first query comes to the last page? "isLoading" field in BoardPagerFragment
            // is set to true, which disables the recyclerview scroll listener to call setNextQuery().
            this.querySnapshot = commentSnapshot;
            mCallback.getFirstQueryResult(commentSnapshot);
        });
    }

    public void setNextQuery() {
        DocumentSnapshot lastVisibleShot = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        log.i("lastVisibleshot: %s", lastVisibleShot.getString("post_title"));

        colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastVisibleShot).limit(Constants.PAGINATION)
                .addSnapshotListener((nextSnapshot, e) -> {

                    if(e != null || nextSnapshot == null) return;
                    log.i("meta data: %s", nextSnapshot.getMetadata().isFromCache());

                    if(nextSnapshot.size() >= Constants.PAGINATION) {
                        log.i("query next: %s", nextSnapshot.size());
                        this.querySnapshot = nextSnapshot;
                        mCallback.getNextQueryResult(nextSnapshot);
                    } else {
                        log.i("query last: %s", nextSnapshot.size());
                        querySnapshot = null;
                        mCallback.getLastQueryResult(nextSnapshot);
                    }
                });
        /*
        query = colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastVisibleShot);
        query.limit(Constants.PAGINATION).addSnapshotListener((nextSnapshot, e) -> {


            if(e != null || nextSnapshot == null) return;

            if(nextSnapshot.size() >= Constants.PAGINATION) {
                this.querySnapshot = nextSnapshot;
                mCallback.getNextQueryResult(nextSnapshot);
            } else {
                querySnapshot = null;
                mCallback.getLastQueryResult(nextSnapshot);
            }

        });

         */
    }
}
